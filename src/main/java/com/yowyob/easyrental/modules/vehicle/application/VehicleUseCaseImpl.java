package com.yowyob.easyrental.modules.vehicle.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.port.in.OrganizationUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleCategoryEntity;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleRequestDTO;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleResponseDTO;
import com.yowyob.easyrental.modules.vehicle.mapper.VehicleMapper;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.CategoryRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleDetailResponseDTO;
import com.yowyob.easyrental.modules.vehicle.dto.PricingUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.ScheduleUpdateDTO;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.pricing.domain.port.in.PricingUseCase;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import com.yowyob.easyrental.modules.review.domain.port.in.ReviewUseCase;
import com.yowyob.easyrental.shared.enums.ResourceType;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.modules.vehicle.domain.port.in.VehicleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleUseCaseImpl implements VehicleUseCase {

    private final VehicleRepositoryPort vehicleRepository;
    private final OrganizationRepositoryPort organizationRepository;
    private final OrganizationUseCase organizationService;
    private final SubscriptionPlanRepositoryPort planRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final VehicleMapper vehicleMapper;
    private final AgencyRepositoryPort agencyRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final ScheduleUseCase scheduleService;
    private final PricingUseCase pricingService;
    private final ReviewUseCase reviewService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Mono<VehicleResponseDTO> createVehicle(UUID orgId, VehicleRequestDTO request) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .switchIfEmpty(Mono.<OrganizationEntity>error(new RuntimeException("Organisation non trouvée")))
                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .flatMap(plan -> {
                            if (org.getCurrentVehicles() >= plan.getMaxVehicles()) {
                                return Mono.<VehicleEntity>error(new RuntimeException(
                                        "Quota de véhicules atteint pour votre plan (" + plan.getName() + ")"));
                            }
                            try {
                                Json functionalitiesJson = Json.of(objectMapper.writeValueAsString(request
                                        .functionalities()));
                                Json engineJson = Json.of(objectMapper.writeValueAsString(request.engineDetails()));
                                Json fuelEfficiencyJson = Json.of(objectMapper.writeValueAsString(request
                                        .fuelEfficiency()));
                                Json insuranceJson = Json.of(objectMapper.writeValueAsString(request
                                        .insuranceDetails()));
                                Json descJson = Json.of(objectMapper.writeValueAsString(request.description()));
                                Json imgsJson = Json.of(objectMapper.writeValueAsString(request.images()));

                                VehicleEntity vehicle = VehicleEntity.builder()
                                        .id(UUID.randomUUID())
                                        .organizationId(orgId)
                                        .agencyId(request.agencyId())
                                        .categoryId(request.categoryId())
                                        .licencePlate(request.licencePlate())
                                        .vinNumber(request.vinNumber())
                                        .brand(request.brand())
                                        .model(request.model())
                                        .yearProduction(request.yearProduction())
                                        .places(request.places())
                                        .kilometrage(request.kilometrage())
                                        .statut(request.statut())
                                        .color(request.color())
                                        .transmission(request.transmission())
                                        .functionalities(functionalitiesJson)
                                        .engineDetails(engineJson)
                                        .fuelEfficiency(fuelEfficiencyJson)
                                        .insuranceDetails(insuranceJson)
                                        .descriptionList(descJson)
                                        .imagesList(imgsJson)
                                        .createdAt(LocalDateTime.now())
                                        .statut("AVAILABLE")
                                        .rating(0.0)
                                        .isNewRecord(true)
                                        .build();

                                return vehicleRepository.save(Objects.requireNonNull(vehicle))
                                        .flatMap(savedVehicle -> {
                                            org.setCurrentVehicles(org.getCurrentVehicles() + 1);
                                            return organizationRepository.save(org)
                                                    .then(updateAgencyVehicleStats(request.agencyId(), 1))
                                                    .thenReturn(savedVehicle);
                                        });
                            } catch (Exception e) {
                                return Mono.error(new RuntimeException("Erreur de sérialisation des données véhicule"));
                            }
                        }))
                .flatMap(this::enrichVehicle)
                .doOnSuccess(v -> eventPublisher.publishEvent(
                        new AuditEvent("CREATE_VEHICLE", "VEHICLE", "Véhicule ajouté : " + v.licencePlate())));
    }

    private Mono<Void> updateAgencyVehicleStats(UUID agencyId, int increment) {
        return agencyRepository.findById(Objects.requireNonNull(agencyId))
                .flatMap(agency -> {
                    agency.setTotalVehicles(agency.getTotalVehicles() + increment);
                    agency.setActiveVehicles(agency.getActiveVehicles() + increment);
                    return agencyRepository.save(agency);
                }).then();
    }

    public Mono<VehicleDetailResponseDTO> getVehicleDetails(UUID id) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
            .flatMap(vehicle -> {
                var scheduleFlux = scheduleService.getResourceSchedule(ResourceType.VEHICLE, id).collectList();
                var reviewsFlux = reviewService.getReviews(ResourceType.VEHICLE, id).collectList();
                Mono<Boolean> orgRequirementMono = organizationRepository.findById(vehicle.getOrganizationId())
                        .map(org -> org.getIsDriverBookingRequired() != null ? org.getIsDriverBookingRequired() : false)
                        .defaultIfEmpty(false);
                Mono<VehicleResponseDTO> vehicleDtoMono = enrichVehicle(vehicle);

                return Mono.zip(vehicleDtoMono, scheduleFlux, reviewsFlux, orgRequirementMono)
                    .map(tuple -> {
                        VehicleResponseDTO vDto = tuple.getT1();
                        return new VehicleDetailResponseDTO(
                            vDto, vDto.pricing(), tuple.getT2(), vehicle.getRating(), tuple.getT3(), tuple.getT4()
                        );
                    });
            });
    }

    @Transactional
    public Mono<VehicleDetailResponseDTO> updateVehiclePricing(UUID id, PricingUpdateDTO request) {
        return vehicleRepository.findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("Véhicule non trouvé")))
            .flatMap(vehicle -> pricingService.setPricing(
                    vehicle.getOrganizationId(), ResourceType.VEHICLE, vehicle.getId(),
                    request.pricePerHour(), request.pricePerDay()
                ).thenReturn(vehicle)
            )
            .flatMap(vehicle -> getVehicleDetails(vehicle.getId()));
    }

    @Transactional
    public Mono<VehicleDetailResponseDTO> updateVehicleSchedules(UUID id, ScheduleUpdateDTO request) {
        return vehicleRepository.findById(id)
            .switchIfEmpty(Mono.error(new RuntimeException("Véhicule non trouvé")))
            .flatMap(vehicle ->
                Flux.fromIterable(request.schedules())
                    .flatMap(schedule -> scheduleService.addUnavailability(
                        vehicle.getOrganizationId(), ResourceType.VEHICLE, vehicle.getId(), schedule
                    ))
                    .then(Mono.just(vehicle))
            )
            .flatMap(vehicle -> getVehicleDetails(vehicle.getId()));
    }

    public Flux<VehicleResponseDTO> getVehiclesByOrg(UUID orgId) {
        return vehicleRepository.findAllByOrganizationId(orgId).flatMap(this::enrichVehicle);
    }

    public Flux<VehicleResponseDTO> getVehiclesByAgency(UUID agencyId) {
        return vehicleRepository.findAllByAgencyId(agencyId).flatMap(this::enrichVehicle);
    }

    public Flux<VehicleResponseDTO> getAvailableVehicles() {
        return vehicleRepository.findAllByStatut("AVAILABLE").flatMap(this::enrichVehicle);
    }

    // NOUVEAU : Service de recherche de véhicules disponibles
    public Flux<VehicleResponseDTO> searchAvailableVehicles(UUID agencyId, UUID categoryId, String keyword) {
        return vehicleRepository.searchAvailableVehicles(
                agencyId,
                categoryId,
                keyword != null && !keyword.isBlank() ? keyword : null
        ).flatMap(this::enrichVehicle);
    }

    // NOUVEAU : Lister les véhicules disponibles d'une agence
    public Flux<VehicleResponseDTO> getAvailableVehiclesByAgency(UUID agencyId) {
        return vehicleRepository.findAllByAgencyIdAndStatut(agencyId, "AVAILABLE")
                .flatMap(this::enrichVehicle);
    }

    public Mono<VehicleResponseDTO> getVehicleById(UUID id) {
        return vehicleRepository.findById(Objects.requireNonNull(id)).flatMap(this::enrichVehicle);
    }

    @Transactional
    public Mono<VehicleResponseDTO> updateVehicle(UUID id, VehicleRequestDTO request) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
            .switchIfEmpty(Mono.error(new RuntimeException("Véhicule non trouvé")))
                .flatMap(v -> {
                    v.setAgencyId(request.agencyId());
                    v.setCategoryId(request.categoryId());
                    v.setLicencePlate(request.licencePlate());
                    v.setVinNumber(request.vinNumber());
                    v.setBrand(request.brand());
                    v.setModel(request.model());
                    v.setYearProduction(request.yearProduction());
                    v.setPlaces(request.places());
                    v.setKilometrage(request.kilometrage());
                    v.setStatut(request.statut());
                    v.setColor(request.color());
                    v.setTransmission(request.transmission());
                    try {
                        v.setFunctionalities(Json.of(objectMapper.writeValueAsString(request.functionalities())));
                        v.setEngineDetails(Json.of(objectMapper.writeValueAsString(request.engineDetails())));
                        v.setFuelEfficiency(Json.of(objectMapper.writeValueAsString(request.fuelEfficiency())));
                        v.setInsuranceDetails(Json.of(objectMapper.writeValueAsString(request.insuranceDetails())));
                        v.setDescriptionList(Json.of(objectMapper.writeValueAsString(request.description())));
                        v.setImagesList(Json.of(objectMapper.writeValueAsString(request.images())));
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Erreur de sérialisation des données véhicule"));
                    }
                    return vehicleRepository.save(v);
                })
                .flatMap(this::enrichVehicle);
    }

    @Transactional
    public Mono<VehicleResponseDTO> updateVehicleStatus(UUID id, String status) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
                .flatMap(v -> {
                    v.setStatut(status.toUpperCase());
                    return vehicleRepository.save(v);
                })
                .flatMap(this::enrichVehicle);
    }

    @Transactional
    public Mono<Void> deleteVehicle(UUID id) {
        return vehicleRepository.findById(Objects.requireNonNull(id))
                .flatMap(v -> vehicleRepository.delete(Objects.requireNonNull(v))
                        .then(organizationService.updateVehicleCounter(v.getOrganizationId(), -1))
                        .then(updateAgencyVehicleStats(v.getAgencyId(), -1)));
    }

    private Mono<VehicleResponseDTO> enrichVehicle(VehicleEntity vehicle) {
        Mono<VehicleCategoryEntity> categoryMono = categoryRepository.findById(Objects.requireNonNull(vehicle
                .getCategoryId()))
                .defaultIfEmpty(VehicleCategoryEntity.builder().name("Unknown").build());

        Mono<PricingEntity> pricingMono = pricingService.getPricing(ResourceType.VEHICLE, vehicle.getId())
                .defaultIfEmpty(new PricingEntity());

        return Mono.zip(categoryMono, pricingMono)
                .map(tuple -> vehicleMapper.toDto(vehicle, tuple.getT1(), tuple.getT2()));
    }

    public Flux<VehicleResponseDTO> getVehiclesByOrgAndCategory(UUID orgId, UUID categoryId) {
        return vehicleRepository.findAllByOrganizationIdAndCategoryId(orgId, categoryId).flatMap(this::enrichVehicle);
    }

    public Flux<VehicleResponseDTO> getVehiclesByAgencyAndCategory(UUID agencyId, UUID categoryId) {
        return vehicleRepository.findAllByAgencyIdAndCategoryId(agencyId, categoryId).flatMap(this::enrichVehicle);
    }
}
