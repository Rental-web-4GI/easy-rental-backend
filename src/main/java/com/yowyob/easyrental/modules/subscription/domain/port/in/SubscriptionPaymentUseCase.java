package com.yowyob.easyrental.modules.subscription.domain.port.in;

import reactor.core.publisher.Mono;

/**
 * Incoming port for subscriptionpayment use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface SubscriptionPaymentUseCase {
    Mono<Boolean> processPayment(String email, String planType, double amount);
}
