package com.yowyob.easyrental.modules.vehicle.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.port.in.OrganizationUseCase;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.pricing.domain.port.in.PricingUseCase;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.review.domain.port.in.ReviewUseCase;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleCategoryEntity;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.CategoryRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.dto.PricingUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.ScheduleUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleDetailResponseDTO;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleRequestDTO;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleResponseDTO;
import com.yowyob.easyrental.modules.vehicle.mapper.VehicleMapper;
import com.yowyob.easyrental.shared.dto.ScheduleRequestDTO;
import com.yowyob.easyrental.shared.enums.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleUseCaseImplTest {

    @Mock private VehicleRepositoryPort vehicleRepository;
    @Mock private OrganizationRepositoryPort organizationRepository;
    @Mock private OrganizationUseCase organizationService;
    @Mock private SubscriptionPlanRepositoryPort planRepository;
    @Mock private CategoryRepositoryPort categoryRepository;
    @Mock private VehicleMapper vehicleMapper;
    @Mock private AgencyRepositoryPort agencyRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ScheduleUseCase scheduleService;
    @Mock private PricingUseCase pricingService;
    @Mock private ReviewUseCase reviewService;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private VehicleUseCaseImpl vehicleUseCase;

    private UUID categoryId;
    private VehicleEntity sampleVehicle;
    private VehicleResponseDTO sampleDto;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        sampleVehicle = VehicleEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .agencyId(UUID.randomUUID())
                .categoryId(categoryId)
                .licencePlate("AB-123")
                .build();
        sampleDto = mock(VehicleResponseDTO.class);
    }

    private void stubEnrichVehicle() {
        when(categoryRepository.findById(categoryId)).thenReturn(Mono.just(
                VehicleCategoryEntity.builder().id(categoryId).name("SUV").build()));
        when(pricingService.getPricing(eq(ResourceType.VEHICLE), any())).thenReturn(Mono.just(new PricingEntity()));
        when(vehicleMapper.toDto(any(), any(), any())).thenReturn(sampleDto);
    }

    @Test
    void shouldReturnErrorWhenVehicleNotFoundOnUpdate() {
        UUID id = UUID.randomUUID();
        when(vehicleRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(vehicleUseCase.updateVehicle(id, mock(VehicleRequestDTO.class)))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldGetVehiclesByOrg() {
        UUID orgId = UUID.randomUUID();
        when(vehicleRepository.findAllByOrganizationId(orgId)).thenReturn(Flux.just(sampleVehicle));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.getVehiclesByOrg(orgId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldUpdateVehicleStatus() {
        UUID id = sampleVehicle.getId();
        when(vehicleRepository.findById(id)).thenReturn(Mono.just(sampleVehicle));
        when(vehicleRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.updateVehicleStatus(id, "rented"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldCreateVehicle() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder()
                .id(orgId).subscriptionPlanId(planId).currentVehicles(0).build();
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder()
                .id(planId).name("FREE").maxVehicles(10).build();
        VehicleRequestDTO request = new VehicleRequestDTO(
                agencyId, categoryId, "AB-123", "VIN1", "Toyota", "Corolla",
                LocalDateTime.now(), 5, 10000.0, "AVAILABLE", "Red", "AUTO",
                null, null, null, null, null, null);

        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(org));
        when(planRepository.findById(planId)).thenReturn(Mono.just(plan));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(vehicleRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(organizationRepository.save(any())).thenReturn(Mono.just(org));
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(
                AgencyEntity.builder().id(agencyId).totalVehicles(0).activeVehicles(0).build()));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.createVehicle(orgId, request))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetVehicleDetails() {
        UUID id = sampleVehicle.getId();
        when(vehicleRepository.findById(id)).thenReturn(Mono.just(sampleVehicle));
        when(scheduleService.getResourceSchedule(ResourceType.VEHICLE, id)).thenReturn(Flux.empty());
        when(reviewService.getReviews(ResourceType.VEHICLE, id)).thenReturn(Flux.empty());
        when(organizationRepository.findById(sampleVehicle.getOrganizationId())).thenReturn(Mono.just(
                OrganizationEntity.builder().id(sampleVehicle.getOrganizationId()).isDriverBookingRequired(false).build()));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.getVehicleDetails(id))
                .expectNextMatches(VehicleDetailResponseDTO.class::isInstance)
                .verifyComplete();
    }

    @Test
    void shouldGetVehicleById() {
        UUID id = sampleVehicle.getId();
        when(vehicleRepository.findById(id)).thenReturn(Mono.just(sampleVehicle));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.getVehicleById(id))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetVehiclesByAgency() {
        UUID agencyId = UUID.randomUUID();
        when(vehicleRepository.findAllByAgencyId(agencyId)).thenReturn(Flux.just(sampleVehicle));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.getVehiclesByAgency(agencyId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetAvailableVehicles() {
        when(vehicleRepository.findAllByStatut("AVAILABLE")).thenReturn(Flux.just(sampleVehicle));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.getAvailableVehicles())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldSearchAvailableVehicles() {
        UUID agencyId = UUID.randomUUID();
        when(vehicleRepository.searchAvailableVehicles(agencyId, categoryId, "toyota"))
                .thenReturn(Flux.just(sampleVehicle));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.searchAvailableVehicles(agencyId, categoryId, "toyota"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetAvailableVehiclesByAgency() {
        UUID agencyId = UUID.randomUUID();
        when(vehicleRepository.findAllByAgencyIdAndStatut(agencyId, "AVAILABLE")).thenReturn(Flux.just(sampleVehicle));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.getAvailableVehiclesByAgency(agencyId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetVehiclesByOrgAndCategory() {
        UUID orgId = UUID.randomUUID();
        when(vehicleRepository.findAllByOrganizationIdAndCategoryId(orgId, categoryId))
                .thenReturn(Flux.just(sampleVehicle));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.getVehiclesByOrgAndCategory(orgId, categoryId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetVehiclesByAgencyAndCategory() {
        UUID agencyId = UUID.randomUUID();
        when(vehicleRepository.findAllByAgencyIdAndCategoryId(agencyId, categoryId))
                .thenReturn(Flux.just(sampleVehicle));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.getVehiclesByAgencyAndCategory(agencyId, categoryId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldUpdateVehiclePricing() {
        UUID id = sampleVehicle.getId();
        when(vehicleRepository.findById(id)).thenReturn(Mono.just(sampleVehicle));
        when(pricingService.setPricing(any(), eq(ResourceType.VEHICLE), eq(id), any(), any()))
                .thenReturn(Mono.just(new PricingEntity()));
        when(scheduleService.getResourceSchedule(ResourceType.VEHICLE, id)).thenReturn(Flux.empty());
        when(reviewService.getReviews(ResourceType.VEHICLE, id)).thenReturn(Flux.empty());
        when(organizationRepository.findById(sampleVehicle.getOrganizationId())).thenReturn(Mono.just(
                OrganizationEntity.builder().id(sampleVehicle.getOrganizationId()).build()));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.updateVehiclePricing(id,
                        new PricingUpdateDTO(BigDecimal.TEN, BigDecimal.valueOf(100))))
                .expectNextMatches(VehicleDetailResponseDTO.class::isInstance)
                .verifyComplete();
    }

    @Test
    void shouldUpdateVehicleSchedules() {
        UUID id = sampleVehicle.getId();
        ScheduleRequestDTO schedule = new ScheduleRequestDTO(
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "MAINTENANCE", "Test");
        when(vehicleRepository.findById(id)).thenReturn(Mono.just(sampleVehicle));
        when(scheduleService.addUnavailability(any(), eq(ResourceType.VEHICLE), eq(id), any()))
                .thenReturn(Mono.just(ScheduleEntity.builder().id(UUID.randomUUID()).build()));
        when(scheduleService.getResourceSchedule(ResourceType.VEHICLE, id)).thenReturn(Flux.empty());
        when(reviewService.getReviews(ResourceType.VEHICLE, id)).thenReturn(Flux.empty());
        when(organizationRepository.findById(sampleVehicle.getOrganizationId())).thenReturn(Mono.just(
                OrganizationEntity.builder().id(sampleVehicle.getOrganizationId()).build()));
        stubEnrichVehicle();

        StepVerifier.create(vehicleUseCase.updateVehicleSchedules(id, new ScheduleUpdateDTO(List.of(schedule))))
                .expectNextMatches(VehicleDetailResponseDTO.class::isInstance)
                .verifyComplete();
    }

    @Test
    void shouldDeleteVehicle() {
        UUID id = sampleVehicle.getId();
        when(vehicleRepository.findById(id)).thenReturn(Mono.just(sampleVehicle));
        when(vehicleRepository.delete(any())).thenReturn(Mono.empty());
        when(organizationService.updateVehicleCounter(sampleVehicle.getOrganizationId(), -1)).thenReturn(Mono.empty());
        when(agencyRepository.findById(sampleVehicle.getAgencyId())).thenReturn(Mono.just(
                AgencyEntity.builder().id(sampleVehicle.getAgencyId()).totalVehicles(1).activeVehicles(1).build()));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(vehicleUseCase.deleteVehicle(id))
                .verifyComplete();
    }
}
