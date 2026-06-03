package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import com.yowyob.easyrental.modules.agency.mapper.AgencyMapper;
import com.yowyob.easyrental.modules.driver.domain.port.in.DriverUseCase;
import com.yowyob.easyrental.modules.notification.domain.port.in.NotificationUseCase;
import com.yowyob.easyrental.modules.notification.dto.NotificationResponseDTO;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.pricing.domain.port.in.PricingUseCase;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.modules.rental.dto.AgencyRentalRequest;
import com.yowyob.easyrental.modules.rental.dto.RentalDetailResponseDTO;
import com.yowyob.easyrental.modules.rental.dto.RentalInitRequest;
import com.yowyob.easyrental.modules.rental.dto.RentalInitResponse;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import com.yowyob.easyrental.modules.vehicle.domain.port.in.VehicleUseCase;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleResponseDTO;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import com.yowyob.easyrental.shared.enums.RentalType;
import com.yowyob.easyrental.shared.enums.ResourceType;
import com.yowyob.easyrental.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RentalUseCaseImplTest {

    @Mock private RentalRepositoryPort rentalRepository;
    @Mock private VehicleRepositoryPort vehicleRepository;
    @Mock private AgencyRepositoryPort agencyRepository;
    @Mock private OrganizationRepositoryPort organizationRepository;
    @Mock private PricingUseCase pricingService;
    @Mock private ScheduleUseCase scheduleService;
    @Mock private NotificationUseCase notificationService;
    @Mock private AgencyMapper agencyMapper;
    @Mock private VehicleUseCase vehicleService;
    @Mock private DriverUseCase driverService;
    @InjectMocks private RentalUseCaseImpl rentalUseCase;

    private UUID vehicleId;
    private UUID agencyId;
    private UUID orgId;
    private VehicleEntity vehicle;
    private AgencyEntity agency;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        agencyId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        vehicle = VehicleEntity.builder().id(vehicleId).agencyId(agencyId).organizationId(orgId).build();
        agency = AgencyEntity.builder().id(agencyId).organizationId(orgId).name("Agency").build();
        doReturn(Mono.just(mock(NotificationResponseDTO.class)))
                .when(notificationService)
                .createNotification(nullable(UUID.class), nullable(UUID.class), any(), any(),
                        nullable(UUID.class), nullable(UUID.class), any(), any(Object[].class));
    }

    @Test
    void shouldReturnErrorWhenRentalNotFound() {
        UUID id = UUID.randomUUID();
        when(rentalRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(rentalUseCase.getRentalDetails(id))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldGetRentalDetails() {
        UUID rentalId = UUID.randomUUID();
        RentalEntity rental = RentalEntity.builder().id(rentalId).vehicleId(vehicleId).agencyId(agencyId).build();
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(vehicleService.getVehicleById(vehicleId)).thenReturn(Mono.just(mock(VehicleResponseDTO.class)));
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));
        when(agencyMapper.toDto(agency)).thenReturn(mock(AgencyResponseDTO.class));

        StepVerifier.create(rentalUseCase.getRentalDetails(rentalId))
                .expectNextMatches(RentalDetailResponseDTO.class::isInstance)
                .verifyComplete();
    }

    @Test
    void shouldGetClientActiveReservations() {
        UUID clientId = UUID.randomUUID();
        RentalEntity rental = RentalEntity.builder().id(UUID.randomUUID()).clientId(clientId)
                .status(RentalStatus.PENDING).build();
        when(rentalRepository.findAllByClientIdAndStatusIn(any(), any())).thenReturn(Flux.just(rental));

        StepVerifier.create(rentalUseCase.getClientActiveReservations(clientId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetClientRentalsHistory() {
        UUID clientId = UUID.randomUUID();
        when(rentalRepository.findAllByClientIdAndStatusIn(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(rentalUseCase.getClientRentalsHistory(clientId))
                .verifyComplete();
    }

    @Test
    void shouldStartRentalWhenPaid() {
        UUID rentalId = UUID.randomUUID();
        RentalEntity rental = RentalEntity.builder().id(rentalId).status(RentalStatus.PAID)
                .amountPaid(BigDecimal.TEN).totalAmount(BigDecimal.TEN).agencyId(agencyId).build();
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rentalUseCase.startRental(rentalId))
                .expectNextMatches(r -> r.getStatus() == RentalStatus.ONGOING)
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenStartRentalNotPaid() {
        UUID rentalId = UUID.randomUUID();
        RentalEntity rental = RentalEntity.builder().id(rentalId).status(RentalStatus.PENDING).build();
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));

        StepVerifier.create(rentalUseCase.startRental(rentalId))
                .expectError(com.yowyob.easyrental.shared.exception.RentalConflictException.class)
                .verify();
    }

    @Test
    void shouldSignalEndRental() {
        UUID rentalId = UUID.randomUUID();
        RentalEntity rental = RentalEntity.builder().id(rentalId).status(RentalStatus.ONGOING)
                .agencyId(agencyId).vehicleId(vehicleId).build();
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rentalUseCase.signalEndRental(rentalId))
                .expectNextMatches(r -> r.getStatus() == RentalStatus.UNDER_REVIEW)
                .verifyComplete();
    }

    @Test
    void shouldValidateReturn() {
        UUID rentalId = UUID.randomUUID();
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        RentalEntity rental = RentalEntity.builder().id(rentalId).status(RentalStatus.UNDER_REVIEW)
                .agencyId(agencyId).vehicleId(vehicleId).endDate(end).clientId(UUID.randomUUID()).build();
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(scheduleService.addUnavailability(any(), any(), any(), any()))
                .thenReturn(Mono.just(ScheduleEntity.builder().id(UUID.randomUUID()).build()));
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rentalUseCase.validateReturn(rentalId))
                .expectNextMatches(r -> r.getStatus() == RentalStatus.COMPLETED)
                .verifyComplete();
    }

    @Test
    void shouldCancelRental() {
        UUID rentalId = UUID.randomUUID();
        RentalEntity rental = RentalEntity.builder().id(rentalId).status(RentalStatus.PENDING)
                .amountPaid(BigDecimal.ZERO).vehicleId(vehicleId).agencyId(agencyId)
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now().plusDays(1)).build();
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(scheduleService.removeScheduleForRental(any(), any(), any(), any())).thenReturn(Mono.empty());
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rentalUseCase.cancelRental(rentalId))
                .expectNextMatches(r -> r.getStatus() == RentalStatus.CANCELLED)
                .verifyComplete();
    }

    @Test
    void shouldInitiateRental() {
        UUID clientId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(2);
        RentalInitRequest request = new RentalInitRequest(
                vehicleId, driverId, start, end, RentalType.DAILY, "690000000");
        PricingEntity vehiclePrice = PricingEntity.builder().pricePerDay(BigDecimal.valueOf(100)).build();
        PricingEntity driverPrice = PricingEntity.builder().pricePerDay(BigDecimal.valueOf(50)).build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Mono.just(vehicle));
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(
                OrganizationEntity.builder().id(orgId).isDriverBookingRequired(true).build()));
        when(pricingService.getPricing(ResourceType.VEHICLE, vehicleId)).thenReturn(Mono.just(vehiclePrice));
        when(pricingService.getPricing(ResourceType.DRIVER, driverId)).thenReturn(Mono.just(driverPrice));
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));
        when(agencyMapper.toDto(agency)).thenReturn(mock(AgencyResponseDTO.class));
        when(rentalRepository.findExistingPendingRental(clientId, vehicleId)).thenReturn(Mono.empty());
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rentalUseCase.initiateRental(clientId, request))
                .expectNextMatches(RentalInitResponse::isAllowed)
                .verifyComplete();
    }

    @Test
    void shouldCreateAgencyRental() {
        UUID driverId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        AgencyRentalRequest request = new AgencyRentalRequest(
                "Client Name", "690000000", "client@test.com", "CNI123",
                vehicleId, driverId, start, end, RentalType.DAILY);
        PricingEntity vehiclePrice = PricingEntity.builder().pricePerDay(BigDecimal.valueOf(100)).build();
        PricingEntity driverPrice = PricingEntity.builder().pricePerDay(BigDecimal.valueOf(50)).build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Mono.just(vehicle));
        when(pricingService.getPricing(ResourceType.VEHICLE, vehicleId)).thenReturn(Mono.just(vehiclePrice));
        when(pricingService.getPricing(ResourceType.DRIVER, driverId)).thenReturn(Mono.just(driverPrice));
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));
        when(agencyMapper.toDto(agency)).thenReturn(mock(AgencyResponseDTO.class));
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(rentalUseCase.createAgencyRental(agencyId, request))
                .expectNextMatches(RentalInitResponse::isAllowed)
                .verifyComplete();
    }

    @Test
    void shouldGetAgencyReservations() {
        when(rentalRepository.findAllByAgencyIdAndStatusIn(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(rentalUseCase.getAgencyReservations(agencyId))
                .verifyComplete();
    }

    @Test
    void shouldGetAgencyRentals() {
        when(rentalRepository.findAllByAgencyIdAndStatusIn(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(rentalUseCase.getAgencyRentals(agencyId))
                .verifyComplete();
    }

    @Test
    void shouldGetOrganizationReservations() {
        when(rentalRepository.findAllByOrganizationIdAndStatusIn(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(rentalUseCase.getOrganizationReservations(orgId))
                .verifyComplete();
    }

    @Test
    void shouldGetOrganizationRentals() {
        when(rentalRepository.findAllByOrganizationIdAndStatusIn(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(rentalUseCase.getOrganizationRentals(orgId))
                .verifyComplete();
    }
}
