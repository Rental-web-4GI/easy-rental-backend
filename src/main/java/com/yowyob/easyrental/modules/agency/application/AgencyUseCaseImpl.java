package com.yowyob.easyrental.modules.agency.application;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.dto.AgencyRequestDTO;
import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import com.yowyob.easyrental.modules.agency.mapper.AgencyMapper;
import com.yowyob.easyrental.modules.agency.infrastructure.adapter.out.persistence.AgencyRepository;
import com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence.OrganizationRepository;
import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionPlanRepository;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.modules.agency.domain.port.in.AgencyUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgencyUseCaseImpl implements AgencyUseCase {

    private final AgencyRepository agencyRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository planRepository;
    private final AgencyMapper agencyMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Mono<AgencyResponseDTO> createAgency(UUID orgId, AgencyRequestDTO request) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
            .switchIfEmpty(Mono.error(new RuntimeException("Organisation non trouvée")))
            .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                .flatMap(plan -> {
                    if (org.getCurrentAgencies() >= plan.getMaxAgencies()) {
                        return Mono.error(new RuntimeException("Quota d'agences atteint pour votre plan (" + plan.getName() + ")"));
                    }

                   AgencyEntity agency = AgencyEntity.builder()
                        .id(UUID.randomUUID())
                        .organizationId(orgId)
                        .name(request.name())
                        .description(request.description())
                        .address(request.address())
                        .city(request.city())
                        .country(request.country() != null ? request.country() : "CM")
                        .postalCode(request.postalCode())
                        .region(request.region())
                        .phone(request.phone())
                        .email(request.email())
                        .managerId(request.managerId())
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .geofenceRadius(request.geofenceRadius() != null ? request.geofenceRadius() : 500.0)
                        .is24Hours(request.is24Hours() != null ? request.is24Hours() : false)
                        .timezone(request.timezone() != null ? request.timezone() : "Africa/Douala")
                        .workingHours(request.workingHours())
                        .allowOnlineBooking(request.allowOnlineBooking() != null ? request.allowOnlineBooking() : true)
                        .depositPercentage(request.depositPercentage())
                        .logoUrl(request.logoUrl())
                        .primaryColor(request.primaryColor())
                        .secondaryColor(request.secondaryColor())
                        .isNewRecord(true)
                        .build();

                    return agencyRepository.save(Objects.requireNonNull(agency))
                            .flatMap(savedAgency -> {
                                org.setCurrentAgencies(org.getCurrentAgencies() + 1);
                                return organizationRepository.save(org)
                                        .thenReturn(savedAgency);
                            });
                }))
            .doOnSuccess(a -> eventPublisher.publishEvent(new AuditEvent("CREATE_AGENCY", "AGENCY", "Agence créée : " + a.getName())))
            .map(agencyMapper::toDto);
    }

    public Mono<Boolean> canAddResource(UUID orgId, String resourceType) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
            .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                .map(plan -> {
                    return switch (resourceType.toUpperCase()) {
                        case "VEHICLE" -> org.getCurrentVehicles() < plan.getMaxVehicles();
                        case "DRIVER" -> org.getCurrentDrivers() < plan.getMaxDrivers();
                        case "USER" -> org.getCurrentUsers() < plan.getMaxUsers();
                        default -> false;
                    };
                }));
    }

    public Flux<AgencyResponseDTO> getAgenciesByOrg(UUID orgId) {
        return agencyRepository.findAllByOrganizationId(orgId)
                .map(agencyMapper::toDto);
    }

    public Flux<AgencyResponseDTO> getAllAgencies() {
        return agencyRepository.findAll().map(agencyMapper::toDto);
    }

    public Mono<AgencyResponseDTO> getAgency(UUID id) {
        return agencyRepository.findById(Objects.requireNonNull(id))
                .map(agencyMapper::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Agence non trouvée")));
    }

    // NOUVEAU : Service de recherche d'agences
    public Flux<AgencyResponseDTO> searchAgencies(String keyword, String city) {
        return agencyRepository.searchAgencies(
                keyword != null && !keyword.isBlank() ? keyword : null,
                city != null && !city.isBlank() ? city : null
        ).map(agencyMapper::toDto);
    }

    @Transactional
    public Mono<AgencyResponseDTO> updateAgency(UUID id, AgencyRequestDTO request) {
        return agencyRepository.findById(Objects.requireNonNull(id))
            .switchIfEmpty(Mono.error(new RuntimeException("Agence non trouvée avec l'ID : " + id)))
                .flatMap(existing -> {
                    if(request.name() != null) existing.setName(request.name());
                    if(request.address() != null) existing.setAddress(request.address());
                    if(request.city() != null) existing.setCity(request.city());
                    if(request.phone() != null) existing.setPhone(request.phone());
                    if(request.email() != null) existing.setEmail(request.email());
                    if(request.description() != null) existing.setDescription(request.description());
                    if(request.postalCode() != null) existing.setPostalCode(request.postalCode());
                    if(request.region() != null) existing.setRegion(request.region());
                    if(request.managerId() != null) existing.setManagerId(request.managerId());
                    if(request.latitude() != null) existing.setLatitude(request.latitude());
                    if(request.longitude() != null) existing.setLongitude(request.longitude());
                    if(request.geofenceRadius() != null) existing.setGeofenceRadius(request.geofenceRadius());
                    if(request.is24Hours() != null) existing.setIs24Hours(request.is24Hours());
                    if(request.timezone() != null) existing.setTimezone(request.timezone());
                    if(request.workingHours() != null) existing.setWorkingHours(request.workingHours());
                    if(request.allowOnlineBooking() != null) existing.setAllowOnlineBooking(request.allowOnlineBooking());
                    if(request.depositPercentage() != null) existing.setDepositPercentage(request.depositPercentage());
                    if(request.logoUrl() != null) existing.setLogoUrl(request.logoUrl());
                    if(request.primaryColor() != null) existing.setPrimaryColor(request.primaryColor());
                    if(request.secondaryColor() != null) existing.setSecondaryColor(request.secondaryColor());
                    return agencyRepository.save(Objects.requireNonNull(existing));
                })
                .doOnSuccess(updated -> eventPublisher.publishEvent(
                        new AuditEvent("UPDATE_AGENCY", "AGENCY", "Updated agency: " + updated.getName())
                ))
                .map(agencyMapper::toDto);
    }

    public Mono<Void> deleteAgency(UUID id) {
        return agencyRepository.deleteById(Objects.requireNonNull(id))
                .doOnSuccess(v -> eventPublisher.publishEvent(
                        new AuditEvent("DELETE_AGENCY", "AGENCY", "Deleted agency ID: " + id)
                ));
    }
}
