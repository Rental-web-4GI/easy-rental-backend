package com.yowyob.easyrental.modules.subscription.application;

import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionPaymentUseCase;
import com.yowyob.easyrental.shared.enums.PaymentMethod;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class SubscriptionPaymentUseCaseImpl implements SubscriptionPaymentUseCase {

  /**
   * Simulates a payment gateway call (MoMo, OM, card, cash at agency).
   * Replace with real billing integration in production.
   */
  public Mono<Boolean> processPayment(String email, String planType, double amount, PaymentMethod method) {
    if (method == null) {
      return Mono.error(new ValidationException("Payment method is required for paid subscription plans"));
    }
    log.info("Payment simulation for {} — plan {} — {} XAF via {}", email, planType, amount, method);

    return Mono.delay(Duration.ofMillis(900))
        .map(ignored -> {
          log.info("Payment validated for {} via {}", email, method);
          return true;
        });
  }
}
