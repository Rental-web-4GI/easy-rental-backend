package com.yowyob.easyrental.modules.subscription.application;

import com.yowyob.easyrental.shared.enums.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SubscriptionPaymentUseCaseImplTest {

    @InjectMocks
    private SubscriptionPaymentUseCaseImpl subscriptionPaymentUseCase;

    @Test
    void shouldProcessPaymentSuccessfully() {
        StepVerifier.create(subscriptionPaymentUseCase.processPayment(
                        "org@test.com", "PRO", 10000.0, PaymentMethod.MOMO))
                .expectNext(true)
                .verifyComplete();
    }
}
