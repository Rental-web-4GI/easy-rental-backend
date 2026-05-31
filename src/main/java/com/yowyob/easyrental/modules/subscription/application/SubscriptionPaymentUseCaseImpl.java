package com.yowyob.easyrental.modules.subscription.application;

import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionPaymentUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Service
@Slf4j
public class SubscriptionPaymentUseCaseImpl implements SubscriptionPaymentUseCase {

    /**
     * Simule un appel à une passerelle de paiement (Stripe, etc.)
     */
    public Mono<Boolean> processPayment(String email, String planType, double amount) {
        log.info("💳 Simulation du paiement pour {} (Plan: {}, Montant: {}XAF)", email, planType, amount);

        // Simule un délai réseau de 800ms sans bloquer le thread
        return Mono.delay(Duration.ofMillis(800))
                .map(d -> {
                    log.info("✅ Paiement validé par la passerelle pour {}", email);
                    return true;
                });
    }
}
