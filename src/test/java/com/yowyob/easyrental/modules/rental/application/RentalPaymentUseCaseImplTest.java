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
import java.util.UUID;

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
}
