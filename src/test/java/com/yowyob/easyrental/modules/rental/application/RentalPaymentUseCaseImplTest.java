package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.notification.domain.port.in.NotificationUseCase;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.domain.port.out.PaymentRepositoryPort;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.modules.rental.dto.PaymentRequest;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import com.yowyob.easyrental.shared.enums.PaymentMethod;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import com.yowyob.easyrental.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RentalPaymentUseCaseImplTest {

    @Mock private PaymentRepositoryPort paymentRepository;
    @Mock private RentalRepositoryPort rentalRepository;
    @Mock private AgencyRepositoryPort agencyRepository;
    @Mock private ScheduleUseCase scheduleService;
    @Mock private NotificationUseCase notificationService;
    @InjectMocks private RentalPaymentUseCaseImpl rentalPaymentUseCase;

    @Test
    void shouldReturnErrorWhenRentalNotFoundForPayment() {
        UUID rentalId = UUID.randomUUID();
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.empty());

        StepVerifier.create(rentalPaymentUseCase.processPayment(
                        rentalId, new PaymentRequest(BigDecimal.valueOf(1000), PaymentMethod.CASH)))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldProcessPartialPayment() {
        UUID rentalId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        RentalEntity rental = RentalEntity.builder().id(rentalId).agencyId(agencyId)
                .status(RentalStatus.PENDING).totalAmount(BigDecimal.valueOf(10000))
                .amountPaid(BigDecimal.ZERO).vehicleId(UUID.randomUUID()).build();
        AgencyEntity agency = AgencyEntity.builder().id(agencyId).monthlyRevenue(0.0).build();

        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(paymentRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));
        when(agencyRepository.save(any())).thenReturn(Mono.just(agency));
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        doReturn(Mono.just(mock(com.yowyob.easyrental.modules.notification.dto.NotificationResponseDTO.class)))
                .when(notificationService)
                .createNotification(nullable(UUID.class), nullable(UUID.class), any(), any(),
                        nullable(UUID.class), nullable(UUID.class), any(), any(Object[].class));

        StepVerifier.create(rentalPaymentUseCase.processPayment(
                        rentalId, new PaymentRequest(BigDecimal.valueOf(1000), PaymentMethod.CASH)))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldConfirmWalkInReservationWithoutClientId() {
        UUID rentalId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        RentalEntity rental = RentalEntity.builder()
                .id(rentalId)
                .agencyId(agencyId)
                .clientId(null)
                .driverId(UUID.randomUUID())
                .vehicleId(UUID.randomUUID())
                .status(RentalStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(10000))
                .amountPaid(BigDecimal.ZERO)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(2))
                .build();
        AgencyEntity agency = AgencyEntity.builder().id(agencyId).monthlyRevenue(0.0).build();

        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(paymentRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));
        when(agencyRepository.save(any())).thenReturn(Mono.just(agency));
        when(scheduleService.addUnavailability(any(), any(), any(), any())).thenReturn(Mono.empty());
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        doReturn(Mono.just(mock(com.yowyob.easyrental.modules.notification.dto.NotificationResponseDTO.class)))
                .when(notificationService)
                .createNotification(nullable(UUID.class), nullable(UUID.class), any(), any(),
                        nullable(UUID.class), nullable(UUID.class), any(), any(Object[].class));

        StepVerifier.create(rentalPaymentUseCase.processPayment(
                        rentalId, new PaymentRequest(BigDecimal.valueOf(6000), PaymentMethod.CASH)))
                .expectNextMatches(saved -> saved.getStatus() == RentalStatus.RESERVED)
                .verifyComplete();
    }

    // --- R2 proportional allocation tests (rental=105000, caution=30000, totalDue=135000) ---

    private RentalEntity r2Rental(UUID rentalId, UUID agencyId) {
        return RentalEntity.builder().id(rentalId).agencyId(agencyId)
                .status(RentalStatus.PENDING)
                .rentalAmount(BigDecimal.valueOf(105000))
                .cautionAmount(BigDecimal.valueOf(30000))
                .totalAmount(BigDecimal.valueOf(135000))
                .amountPaid(BigDecimal.ZERO)
                .rentalAmountPaid(BigDecimal.ZERO)
                .cautionAmountPaid(BigDecimal.ZERO)
                .cautionHeld(BigDecimal.ZERO)
                .vehicleId(UUID.randomUUID())
                .build();
    }

    private void stubCollaborators(UUID rentalId, UUID agencyId, RentalEntity rental, AgencyEntity agency) {
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(paymentRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(scheduleService.addUnavailability(any(), any(), any(), any())).thenReturn(Mono.empty());
        when(rentalRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        doReturn(Mono.just(mock(com.yowyob.easyrental.modules.notification.dto.NotificationResponseDTO.class)))
                .when(notificationService)
                .createNotification(nullable(UUID.class), nullable(UUID.class), any(), any(),
                        nullable(UUID.class), nullable(UUID.class), any(), any(Object[].class));
    }

    @Test
    void partialPayment_60pct_transitionsToReserved() {
        UUID rentalId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        RentalEntity rental = r2Rental(rentalId, agencyId);
        AgencyEntity agency = AgencyEntity.builder().id(agencyId).monthlyRevenue(0.0)
                .cautionEscrowBalance(BigDecimal.ZERO).build();
        stubCollaborators(rentalId, agencyId, rental, agency);

        StepVerifier.create(rentalPaymentUseCase.processPayment(
                        rentalId, new PaymentRequest(BigDecimal.valueOf(81000), PaymentMethod.CASH)))
                .expectNextMatches(saved -> saved.getStatus() == RentalStatus.RESERVED
                        && saved.getRentalAmountPaid().compareTo(BigDecimal.valueOf(63000)) == 0
                        && saved.getCautionAmountPaid().compareTo(BigDecimal.valueOf(18000)) == 0)
                .verifyComplete();

        assertThat(agency.getMonthlyRevenue()).isEqualTo(63000.0);
        assertThat(agency.getCautionEscrowBalance()).isEqualByComparingTo("18000");
    }

    @Test
    void fullPayment_transitionsToPaid() {
        UUID rentalId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        RentalEntity rental = r2Rental(rentalId, agencyId);
        AgencyEntity agency = AgencyEntity.builder().id(agencyId).monthlyRevenue(0.0)
                .cautionEscrowBalance(BigDecimal.ZERO).build();
        stubCollaborators(rentalId, agencyId, rental, agency);

        StepVerifier.create(rentalPaymentUseCase.processPayment(
                        rentalId, new PaymentRequest(BigDecimal.valueOf(135000), PaymentMethod.CASH)))
                .expectNextMatches(saved -> saved.getStatus() == RentalStatus.PAID)
                .verifyComplete();

        assertThat(agency.getMonthlyRevenue()).isEqualTo(105000.0);
        assertThat(agency.getCautionEscrowBalance()).isEqualByComparingTo("30000");
    }

    @Test
    void splitPayments_soldeToPaid() {
        UUID rentalId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        RentalEntity rental = r2Rental(rentalId, agencyId);
        AgencyEntity agency = AgencyEntity.builder().id(agencyId).monthlyRevenue(0.0)
                .cautionEscrowBalance(BigDecimal.ZERO).build();
        stubCollaborators(rentalId, agencyId, rental, agency);

        StepVerifier.create(rentalPaymentUseCase.processPayment(
                        rentalId, new PaymentRequest(BigDecimal.valueOf(81000), PaymentMethod.CASH)))
                .expectNextMatches(saved -> saved.getStatus() == RentalStatus.RESERVED)
                .verifyComplete();

        StepVerifier.create(rentalPaymentUseCase.processPayment(
                        rentalId, new PaymentRequest(BigDecimal.valueOf(54000), PaymentMethod.CASH)))
                .expectNextMatches(saved -> saved.getStatus() == RentalStatus.PAID
                        && saved.getRentalAmountPaid().compareTo(BigDecimal.valueOf(105000)) == 0
                        && saved.getCautionAmountPaid().compareTo(BigDecimal.valueOf(30000)) == 0)
                .verifyComplete();

        assertThat(agency.getMonthlyRevenue()).isEqualTo(105000.0);
        assertThat(agency.getCautionEscrowBalance()).isEqualByComparingTo("30000");
    }

    @Test
    void escrow_stays_out_of_revenue() {
        // Across all 3 scenarios above, agency.monthlyRevenue must equal sum(rentalPortion),
        // never sum(amount) — i.e. it must never include the caution portion.
        UUID rentalId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        RentalEntity rental = r2Rental(rentalId, agencyId);
        AgencyEntity agency = AgencyEntity.builder().id(agencyId).monthlyRevenue(0.0)
                .cautionEscrowBalance(BigDecimal.ZERO).build();
        stubCollaborators(rentalId, agencyId, rental, agency);

        StepVerifier.create(rentalPaymentUseCase.processPayment(
                        rentalId, new PaymentRequest(BigDecimal.valueOf(81000), PaymentMethod.CASH)))
                .expectNextCount(1)
                .verifyComplete();
        StepVerifier.create(rentalPaymentUseCase.processPayment(
                        rentalId, new PaymentRequest(BigDecimal.valueOf(54000), PaymentMethod.CASH)))
                .expectNextCount(1)
                .verifyComplete();

        // Total paid in = 135000, but only the rentalAmount share (105000) may land in revenue.
        assertThat(agency.getMonthlyRevenue()).isEqualTo(105000.0);
        assertThat(agency.getMonthlyRevenue()).isNotEqualTo(135000.0);
    }
}
