package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.out.AuthUserPort;
import com.yowyob.easyrental.modules.rental.domain.PaymentEntity;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.domain.port.in.RentalUseCase;
import com.yowyob.easyrental.modules.rental.domain.port.out.PaymentRepositoryPort;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionRepositoryPort;
import com.yowyob.easyrental.shared.enums.PaymentMethod;
import com.yowyob.easyrental.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionUseCaseImplTest {

    @Mock private PaymentRepositoryPort paymentRepository;
    @Mock private SubscriptionRepositoryPort subscriptionRepository;
    @Mock private SubscriptionPlanRepositoryPort planRepository;
    @Mock private RentalRepositoryPort rentalRepository;
    @Mock private RentalUseCase rentalUseCase;
    @Mock private AuthUserPort authUserPort;
    @InjectMocks private TransactionUseCaseImpl transactionUseCase;

    @Test
    void shouldReturnErrorWhenTransactionNotFound() {
        UUID id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Mono.empty());
        when(subscriptionRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(transactionUseCase.getTransactionDetails(id))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldGetClientTransactions() {
        UUID clientId = UUID.randomUUID();
        UUID rentalId = UUID.randomUUID();
        PaymentEntity payment = PaymentEntity.builder().id(UUID.randomUUID()).rentalId(rentalId)
                .amount(BigDecimal.TEN).transactionDate(LocalDateTime.now()).transactionRef("TX-1")
                .paymentMethod(PaymentMethod.CASH).build();
        RentalEntity rental = RentalEntity.builder().id(rentalId).build();

        when(paymentRepository.findAllByClientId(clientId)).thenReturn(Flux.just(payment));
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));

        StepVerifier.create(transactionUseCase.getClientTransactions(clientId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetClientTransactionsByEmail() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("c@test.com").build();
        when(authUserPort.findByEmail("c@test.com")).thenReturn(Mono.just(user));
        when(paymentRepository.findAllByClientId(user.getId())).thenReturn(Flux.empty());

        StepVerifier.create(transactionUseCase.getClientTransactionsByEmail("c@test.com"))
                .verifyComplete();
    }

    @Test
    void shouldGetAgencyTransactions() {
        UUID agencyId = UUID.randomUUID();
        UUID rentalId = UUID.randomUUID();
        PaymentEntity payment = PaymentEntity.builder().id(UUID.randomUUID()).rentalId(rentalId)
                .amount(BigDecimal.TEN).transactionDate(LocalDateTime.now()).transactionRef("TX-2")
                .paymentMethod(PaymentMethod.CASH).build();
        RentalEntity rental = RentalEntity.builder().id(rentalId).build();

        when(paymentRepository.findAllByAgencyId(agencyId)).thenReturn(Flux.just(payment));
        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));

        StepVerifier.create(transactionUseCase.getAgencyTransactions(agencyId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetOrganizationTransactions() {
        UUID orgId = UUID.randomUUID();
        when(paymentRepository.findAllRentalPaymentsByOrganizationId(orgId)).thenReturn(Flux.empty());
        when(subscriptionRepository.findAllByOrganizationIdOrderByStartDateDesc(orgId)).thenReturn(Flux.empty());

        StepVerifier.create(transactionUseCase.getOrganizationTransactions(orgId))
                .verifyComplete();
    }
}
