package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import com.yowyob.easyrental.modules.agency.mapper.AgencyMapper;
import com.yowyob.easyrental.modules.auth.domain.port.out.AuthUserPort;
import com.yowyob.easyrental.modules.driver.domain.port.in.DriverUseCase;
import com.yowyob.easyrental.modules.inspection.domain.port.in.InspectionUseCase;
import com.yowyob.easyrental.modules.inspection.dto.InspectionCreateRequest;
import com.yowyob.easyrental.modules.inspection.dto.InspectionResponseDTO;
import com.yowyob.easyrental.modules.notification.domain.port.in.NotificationUseCase;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.pricing.domain.port.in.PricingUseCase;
import com.yowyob.easyrental.modules.rental.domain.PaymentEntity;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.domain.port.in.RentalPaymentUseCase;
import com.yowyob.easyrental.modules.rental.domain.port.out.PaymentRepositoryPort;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.modules.rental.dto.CheckInRequest;
import com.yowyob.easyrental.modules.rental.dto.CheckoutSettlementRequest;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import com.yowyob.easyrental.modules.tracking.domain.port.in.TrackingUseCase;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import com.yowyob.easyrental.modules.vehicle.domain.port.in.VehicleUseCase;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import com.yowyob.easyrental.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RentalUseCaseImplCheckoutTest {

    @Mock private RentalRepositoryPort rentalRepository;
    @Mock private VehicleRepositoryPort vehicleRepository;
    @Mock private AgencyRepositoryPort agencyRepository;
    @Mock private OrganizationRepositoryPort organizationRepository;
    @Mock private PricingUseCase pricingService;
    @Mock private ScheduleUseCase scheduleService;
    @Mock private NotificationUseCase notificationService;
    @Mock private RentalPaymentUseCase rentalPaymentUseCase;
    @Mock private AgencyMapper agencyMapper;
    @Mock private VehicleUseCase vehicleService;
    @Mock private DriverUseCase driverService;
    @Mock private AuthUserPort authUserPort;
    @Mock private PaymentRepositoryPort paymentRepository;
    @Mock private InspectionUseCase inspectionUseCase;
    @Mock private TrackingUseCase trackingUseCase;

    private RentalUseCaseImpl useCase;

    private final UUID rentalId = UUID.randomUUID();
    private final UUID agencyId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new RentalUseCaseImpl(
                rentalRepository, vehicleRepository, agencyRepository, organizationRepository,
                pricingService, scheduleService, notificationService, rentalPaymentUseCase,
                agencyMapper, vehicleService, driverService, authUserPort,
                paymentRepository, inspectionUseCase, trackingUseCase);
    }

    private RentalEntity rental(RentalStatus status) {
        return RentalEntity.builder()
                .id(rentalId)
                .agencyId(agencyId)
                .vehicleId(vehicleId)
                .status(status)
                .cautionHeld(new BigDecimal("30000.00"))
                .cautionAmount(new BigDecimal("30000.00"))
                .rentalAmount(new BigDecimal("105000.00"))
                .startOdometer(50000)
                .build();
    }

    private InspectionCreateRequest inspectionPayload() {
        return new InspectionCreateRequest("CHECK_IN", 50000, (short) 6, "ok",
                List.of("p1", "p2", "p3", "p4"), null);
    }

    // stub getRentalDetails dependencies (vehicle/driver/agency lookups)
    private void stubDetails() {
        when(vehicleService.getVehicleById(any())).thenReturn(Mono.empty());
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.empty());
    }

    @Test
    void checkIn_notPaid_throws() {
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental(RentalStatus.RESERVED)));

        StepVerifier.create(useCase.checkIn(rentalId, new CheckInRequest(50000, inspectionPayload())))
                .expectErrorMatches(e -> e instanceof ValidationException
                        && e.getMessage().equals("MUST_BE_PAID"))
                .verify();
    }

    @Test
    void checkIn_paid_createsInspectionAndTransitionsToOngoing() {
        RentalEntity r = rental(RentalStatus.PAID);
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(r));
        when(inspectionUseCase.createInspection(any(), any()))
                .thenReturn(Mono.just(mockInspectionDto()));
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        stubDetails();

        StepVerifier.create(useCase.checkIn(rentalId, new CheckInRequest(50000, inspectionPayload())))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<InspectionCreateRequest> inspCaptor =
                ArgumentCaptor.forClass(InspectionCreateRequest.class);
        verify(inspectionUseCase).createInspection(any(), inspCaptor.capture());
        assertThat(inspCaptor.getValue().type()).isEqualTo("CHECK_IN");

        ArgumentCaptor<RentalEntity> saveCaptor = ArgumentCaptor.forClass(RentalEntity.class);
        verify(rentalRepository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getStatus()).isEqualTo(RentalStatus.ONGOING);
        assertThat(saveCaptor.getValue().getStartOdometer()).isEqualTo(50000);
    }

    @Test
    void signalEnd_notOngoing_throws() {
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental(RentalStatus.PAID)));

        StepVerifier.create(useCase.signalEnd(rentalId))
                .expectErrorMatches(e -> e instanceof ValidationException
                        && e.getMessage().equals("NOT_ONGOING"))
                .verify();
    }

    @Test
    void settleReturn_deductionOverEscrow_throws() {
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental(RentalStatus.UNDER_REVIEW)));

        StepVerifier.create(useCase.settleReturn(rentalId,
                        new CheckoutSettlementRequest(new BigDecimal("40000"), "damage")))
                .expectErrorMatches(e -> e instanceof ValidationException
                        && e.getMessage().equals("DEDUCTION_EXCEEDS_ESCROW"))
                .verify();
    }

    @Test
    void settleReturn_deductionWithoutReason_throws() {
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental(RentalStatus.UNDER_REVIEW)));

        StepVerifier.create(useCase.settleReturn(rentalId,
                        new CheckoutSettlementRequest(new BigDecimal("5000"), null)))
                .expectErrorMatches(e -> e instanceof ValidationException
                        && e.getMessage().equals("REASON_REQUIRED"))
                .verify();
    }

    @Test
    void settleReturn_normal_createsTwoPaymentsAndCompletes() {
        RentalEntity r = rental(RentalStatus.UNDER_REVIEW);
        r.setEndOdometer(50350);
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(r));
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(paymentRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        AgencyEntity agency = AgencyEntity.builder().id(agencyId)
                .cautionEscrowBalance(new BigDecimal("30000.00")).build();
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(agencyMapper.toDto(any())).thenReturn(mock(AgencyResponseDTO.class));
        VehicleEntity vehicle = VehicleEntity.builder().id(vehicleId).kilometrage(50000.0).build();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Mono.just(vehicle));
        when(vehicleRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(vehicleService.getVehicleById(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.settleReturn(rentalId,
                        new CheckoutSettlementRequest(new BigDecimal("5000"), "rayure portière")))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<PaymentEntity> payCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository, times(2)).save(payCaptor.capture());
        List<PaymentEntity> payments = payCaptor.getAllValues();
        assertThat(payments).anyMatch(p -> "CAUTION_REFUND".equals(p.getPaymentCategory())
                && p.getAmount().compareTo(new BigDecimal("25000.00")) == 0);
        assertThat(payments).anyMatch(p -> "CAUTION_RETENTION".equals(p.getPaymentCategory())
                && p.getAmount().compareTo(new BigDecimal("5000")) == 0);

        ArgumentCaptor<RentalEntity> rentalCaptor = ArgumentCaptor.forClass(RentalEntity.class);
        verify(rentalRepository).save(rentalCaptor.capture());
        assertThat(rentalCaptor.getValue().getStatus()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(rentalCaptor.getValue().getCautionDeducted()).isEqualByComparingTo("5000");
        assertThat(rentalCaptor.getValue().getCautionRefunded()).isEqualByComparingTo("25000.00");

        ArgumentCaptor<AgencyEntity> agencyCaptor = ArgumentCaptor.forClass(AgencyEntity.class);
        verify(agencyRepository).save(agencyCaptor.capture());
        assertThat(agencyCaptor.getValue().getCautionEscrowBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void settleReturn_zeroDeduction_singleRefundPayment() {
        RentalEntity r = rental(RentalStatus.UNDER_REVIEW);
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(r));
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(paymentRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        AgencyEntity agency = AgencyEntity.builder().id(agencyId)
                .cautionEscrowBalance(new BigDecimal("30000.00")).build();
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(agencyMapper.toDto(any())).thenReturn(mock(AgencyResponseDTO.class));
        when(vehicleService.getVehicleById(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.settleReturn(rentalId,
                        new CheckoutSettlementRequest(BigDecimal.ZERO, null)))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<PaymentEntity> payCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository, times(1)).save(payCaptor.capture());
        assertThat(payCaptor.getValue().getPaymentCategory()).isEqualTo("CAUTION_REFUND");
        assertThat(payCaptor.getValue().getAmount()).isEqualByComparingTo("30000.00");
    }

    private InspectionResponseDTO mockInspectionDto() {
        return new InspectionResponseDTO(UUID.randomUUID(), rentalId, "CHECK_IN",
                50000, (short) 6, "ok", List.of("p1", "p2", "p3", "p4"), null, null, null);
    }
}
