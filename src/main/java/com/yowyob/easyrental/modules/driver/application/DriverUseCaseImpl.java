package com.yowyob.easyrental.modules.driver.application;

import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.driver.domain.DriverEntity;
import com.yowyob.easyrental.modules.driver.dto.DriverResponseDTO;
import com.yowyob.easyrental.modules.driver.mapper.DriverMapper;
import com.yowyob.easyrental.modules.driver.domain.port.out.DriverRepositoryPort;
import com.yowyob.easyrental.modules.media.domain.MediaEntity;
import com.yowyob.easyrental.modules.media.domain.port.in.MediaUseCase;
import com.yowyob.easyrental.modules.organization.domain.port.in.OrganizationUseCase;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.driver.dto.DriverDetailResponseDTO;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.pricing.domain.port.in.PricingUseCase;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import com.yowyob.easyrental.modules.vehicle.dto.PricingUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.ScheduleUpdateDTO;
import com.yowyob.easyrental.modules.review.domain.port.in.ReviewUseCase;
import com.yowyob.easyrental.shared.enums.ResourceType;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.modules.driver.domain.port.in.DriverUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverUseCaseImpl implements DriverUseCase {

    private final DriverRepositoryPort driverRepository;
    private final AgencyRepositoryPort agencyRepository;
    private final OrganizationUseCase organizationService;
    private final OrganizationRepositoryPort organizationRepository;
    private final MediaUseCase mediaService;
    private final DriverMapper driverMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduleUseCase scheduleService;
    private final PricingUseCase pricingService;
    private final ReviewUseCase reviewService;

    public Mono<DriverResponseDTO> createDriver(
            UUID orgId,
            UUID agencyId,
            String firstname, String lastname, String tel, Integer age, Integer gender,
            String cniNumber, String licenseNumber, java.time.LocalDate licenseExpiry, Integer yearsExperience,
            FilePart profilFile, FilePart cniFile, FilePart licenseFile,
            BigDecimal pricePerHour, BigDecimal pricePerDay, BigDecimal pricePerMonth) {

        return organizationService.validateQuota(orgId, "DRIVER")
            .flatMap(hasQuota -> {
                if (!hasQuota) {
                    return Mono.error(new RuntimeException("Quota de chauffeurs atteint pour votre plan."));
                }

                // Upload documents first (disk I/O) — avoid holding DB transactions during large PDF transfers.
                return uploadDriverDocuments(profilFile, cniFile, licenseFile)
                    .flatMap(urls -> persistDriver(
                            orgId,
                            agencyId,
                            firstname,
                            lastname,
                            tel,
                            age,
                            gender,
                            cniNumber,
                            licenseNumber,
                            licenseExpiry,
                            yearsExperience,
                            urls,
                            pricePerHour,
                            pricePerDay,
                            pricePerMonth));
            })
            .doOnSuccess(d -> eventPublisher.publishEvent(new AuditEvent("CREATE_DRIVER", "DRIVER",
                    "Conducteur créé : " + d.getFirstname() + " " + d.getLastname())))
            .flatMap(this::enrichDriver);
    }

    private Mono<DriverDocumentUrls> uploadDriverDocuments(
            FilePart profilFile,
            FilePart cniFile,
            FilePart licenseFile) {
        return mediaService.uploadFile(profilFile)
                .map(MediaEntity::getFileUrl)
                .flatMap(profilUrl -> mediaService.uploadFile(cniFile)
                        .map(MediaEntity::getFileUrl)
                        .flatMap(cniUrl -> mediaService.uploadFile(licenseFile)
                                .map(MediaEntity::getFileUrl)
                                .map(licenseUrl -> new DriverDocumentUrls(profilUrl, cniUrl, licenseUrl))));
    }

    private Mono<DriverEntity> persistDriver(
            UUID orgId,
            UUID agencyId,
            String firstname,
            String lastname,
            String tel,
            Integer age,
            Integer gender,
            String cniNumber,
            String licenseNumber,
            java.time.LocalDate licenseExpiry,
            Integer yearsExperience,
            DriverDocumentUrls urls,
            BigDecimal pricePerHour,
            BigDecimal pricePerDay,
            BigDecimal pricePerMonth) {

        DriverEntity driver = DriverEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .agencyId(agencyId)
                .firstname(firstname)
                .lastname(lastname)
                .tel(tel)
                .age(age)
                .gender(gender)
                .profilUrl(urls.profilUrl())
                .cniUrl(urls.cniUrl())
                .drivingLicenseUrl(urls.licenseUrl())
                .cniNumber(cniNumber)
                .licenseNumber(licenseNumber)
                .licenseExpiry(licenseExpiry)
                .yearsExperience(yearsExperience)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .rating(0.0)
                .isNewRecord(true)
                .build();

        return driverRepository.save(driver)
                .flatMap(saved -> {
                    if (saved == null) {
                        return Mono.error(new RuntimeException("Failed to save driver"));
                    }
                    return organizationService.updateDriverCounter(orgId, 1)
                            .then(updateAgencyDriverStats(agencyId, 1))
                            .thenReturn(saved);
                })
                .flatMap(saved -> applyInitialPricing(orgId, saved, pricePerHour, pricePerDay, pricePerMonth));
    }

    private record DriverDocumentUrls(String profilUrl, String cniUrl, String licenseUrl) {}

    public Flux<DriverResponseDTO> getDriversByOrg(UUID orgId) {
        return driverRepository.findAllByOrganizationId(orgId)
                .flatMap(this::enrichDriver);
    }

    public Mono<DriverDetailResponseDTO> getDriverDetails(UUID id) {
        return driverRepository.findById(Objects.requireNonNull(id))
            .switchIfEmpty(Mono.error(new RuntimeException("Driver not found")))
            .flatMap(driver -> {
                // On récupère le DTO enrichi (avec prix)
                Mono<DriverResponseDTO> dtoMono = enrichDriver(driver);

                var pricingMono = pricingService.getPricing(ResourceType.DRIVER, id);
                var scheduleFlux = scheduleService.getResourceSchedule(ResourceType.DRIVER, id).collectList();
                var reviewsFlux = reviewService.getReviews(ResourceType.DRIVER, id).collectList();

                Mono<Boolean> orgRequirementMono = organizationRepository.findById(driver.getOrganizationId())
                        .map(org -> org.getIsDriverBookingRequired() != null ? org.getIsDriverBookingRequired() : false)
                        .defaultIfEmpty(false);

                return Mono.zip(dtoMono, pricingMono.defaultIfEmpty(new PricingEntity()), scheduleFlux,
                        reviewsFlux, orgRequirementMono)
                    .map(tuple -> {
                        PricingEntity pricingEntity = tuple.getT2();
                        if (pricingEntity.getId() == null && tuple.getT1().pricing() != null
                                && tuple.getT1().pricing().getId() != null) {
                            pricingEntity = tuple.getT1().pricing();
                        }
                        return new DriverDetailResponseDTO(
                        tuple.getT1(),
                        pricingEntity.getId() == null ? null : pricingEntity,
                        tuple.getT3(),
                        driver.getRating(),
                        tuple.getT4(),
                        tuple.getT5()
                    );
                    });
            });
    }

    public Flux<DriverResponseDTO> getDriversByAgency(UUID agencyId) {
        return driverRepository.findAllByAgencyId(agencyId)
                .flatMap(this::enrichDriver);
    }

    /**
     * Récupère les chauffeurs disponibles pour une agence sur une plage horaire
     * AVEC LEUR PRIX
     */
    public Flux<DriverResponseDTO> getAvailableDrivers(UUID agencyId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            return Flux.error(new IllegalArgumentException("La date de début doit être avant la date de fin"));
        }
        return driverRepository.findAvailableDrivers(agencyId, startDate, endDate)
                .flatMap(this::enrichDriver);
    }

    public Mono<DriverResponseDTO> getDriverById(UUID id) {
        return driverRepository.findById(Objects.requireNonNull(id))
                .flatMap(this::enrichDriver)
                .switchIfEmpty(Mono.error(new RuntimeException("Conducteur non trouvé")));
    }

    @Transactional
    public Mono<DriverResponseDTO> changeAgency(UUID driverId, UUID newAgencyId) {
        return driverRepository.findById(Objects.requireNonNull(driverId))
                .flatMap(driver -> {
                    UUID oldAgencyId = driver.getAgencyId();
                    if (oldAgencyId.equals(newAgencyId)) {
                        return Mono.just(driver);
                    }

                    driver.setAgencyId(newAgencyId);
                    driver.setUpdatedAt(LocalDateTime.now());

                    return updateAgencyDriverStats(oldAgencyId, -1)
                            .then(updateAgencyDriverStats(newAgencyId, 1))
                            .then(driverRepository.save(driver));
                })
                .flatMap(this::enrichDriver);
    }

    @Transactional
    public Mono<DriverDetailResponseDTO> updateDriverPricing(UUID id, PricingUpdateDTO request) {
        return driverRepository.findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("Chauffeur non trouvé")))
            .flatMap(driver -> pricingService.setPricing(
                    driver.getOrganizationId(), ResourceType.DRIVER, driver.getId(),
                    request.pricePerHour(), request.pricePerDay(), request.pricePerMonth()
                ).thenReturn(driver)
            )
            .flatMap(driver -> getDriverDetails(driver.getId()));
    }

    @Transactional
    public Mono<DriverDetailResponseDTO> updateDriverSchedules(UUID id, ScheduleUpdateDTO request) {
        return driverRepository.findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("Chauffeur non trouvé")))
            .flatMap(driver ->
                Flux.fromIterable(request.schedules())
                    .flatMap(schedule -> scheduleService.addUnavailability(
                        driver.getOrganizationId(), ResourceType.DRIVER, driver.getId(), schedule
                    ))
                    .then(Mono.just(driver))
            )
            .flatMap(driver -> getDriverDetails(driver.getId()));
    }

    @Transactional
    public Mono<DriverResponseDTO> updateDriverStatus(UUID id, String status) {
        return driverRepository.findById(Objects.requireNonNull(id))
                .switchIfEmpty(Mono.error(new RuntimeException("Chauffeur non trouvé")))
                .flatMap(driver -> {
                    driver.setStatus(status);
                    driver.setUpdatedAt(LocalDateTime.now());
                    return driverRepository.save(driver);
                })
                .flatMap(this::enrichDriver);
    }

    @Transactional
    public Mono<Void> deleteDriver(UUID id) {
        return driverRepository.findById(Objects.requireNonNull(id))
                .flatMap(driver -> driverRepository.delete(Objects.requireNonNull(driver))
                        .then(organizationService.updateDriverCounter(driver.getOrganizationId(), -1))
                        .then(updateAgencyDriverStats(driver.getAgencyId(), -1)));
    }

    private Mono<Void> updateAgencyDriverStats(UUID agencyId, int increment) {
        return agencyRepository.findById(Objects.requireNonNull(agencyId))
                .flatMap(agency -> {
                    agency.setTotalDrivers(agency.getTotalDrivers() + increment);
                    agency.setActiveDrivers(agency.getActiveDrivers() + increment);
                    return agencyRepository.save(agency);
                }).then();
    }

    private Mono<DriverEntity> applyInitialPricing(
            UUID orgId,
            DriverEntity driver,
            BigDecimal pricePerHour,
            BigDecimal pricePerDay,
            BigDecimal pricePerMonth) {
        if (!hasAnyPrice(pricePerHour, pricePerDay, pricePerMonth)) {
            return Mono.just(driver);
        }
        return pricingService.setPricing(
                orgId,
                ResourceType.DRIVER,
                driver.getId(),
                pricePerHour,
                pricePerDay,
                pricePerMonth
        ).thenReturn(driver);
    }

    private boolean hasAnyPrice(BigDecimal pricePerHour, BigDecimal pricePerDay, BigDecimal pricePerMonth) {
        return isPositive(pricePerHour) || isPositive(pricePerDay) || isPositive(pricePerMonth);
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    // --- METHODE PRIVEE POUR ENRICHIR LE DTO AVEC LE PRIX ---
    private Mono<DriverResponseDTO> enrichDriver(DriverEntity driver) {
        return pricingService.getPricing(ResourceType.DRIVER, driver.getId())
                .defaultIfEmpty(new PricingEntity()) // Objet vide si pas de prix défini
                .map(pricing -> driverMapper.toDto(driver, pricing));
    }
}
