package com.yowyob.easyrental.kernel.infrastructure.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Skeleton consumer for kernel domain events (org/user/resource).
 * Enable with {@code easy-rental.kafka.kernel-events.enabled=true} in prod.
 *
 * @author Easy Rental Team
 * @since 2026-07-08
 */
@Component
@ConditionalOnProperty(prefix = "easy-rental.kafka.kernel-events", name = "enabled", havingValue = "true")
public class KernelDomainEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(KernelDomainEventListener.class);

    @KafkaListener(
            topics = "${easy-rental.kafka.kernel-events.topic:kernel.domain.events}",
            groupId = "${easy-rental.kafka.kernel-events.group-id:easy-rental-backend}")
    public void onKernelEvent(String payload) {
        LOGGER.info("Received kernel domain event (no-op handler): {}", truncate(payload));
    }

    private static String truncate(String payload) {
        if (payload == null) {
            return "";
        }
        return payload.length() > 500 ? payload.substring(0, 500) + "…" : payload;
    }
}
