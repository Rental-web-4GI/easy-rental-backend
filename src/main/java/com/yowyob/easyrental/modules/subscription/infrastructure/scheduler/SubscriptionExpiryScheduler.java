package com.yowyob.easyrental.modules.subscription.infrastructure.scheduler;

import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Proactively downgrades expired paid subscriptions to FREE.
 *
 * @author Easy Rental Team
 * @since 2026-07-07
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryScheduler {

    private final SubscriptionUseCase subscriptionUseCase;

    @Scheduled(cron = "0 0 * * * *")
    public void runHourlyExpiryCheck() {
        subscriptionUseCase.processExpiredSubscriptions()
                .doOnSuccess(count -> {
                    if (count != null && count > 0) {
                        log.info("Auto-downgraded {} expired organization subscription(s) to FREE", count);
                    }
                })
                .doOnError(error -> log.error("Subscription expiry job failed", error))
                .subscribe();
    }
}
