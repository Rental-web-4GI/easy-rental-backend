package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.yowyob.easyrental.kernel.domain.port.out.KernelAuditPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Logging / no-op audit bridge. Enable with {@code easy-rental.audit.kernel-sync.enabled=true}.
 *
 * @author Easy Rental Team
 * @since 2026-07-08
 */
@Component
@ConditionalOnProperty(prefix = "easy-rental.audit.kernel-sync", name = "enabled", havingValue = "true")
public class KernelAuditAdapter implements KernelAuditPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(KernelAuditAdapter.class);

    @Override
    public Mono<Void> publish(Map<String, Object> auditPayload) {
        LOGGER.debug("Kernel audit sync (stub): {}", auditPayload);
        return Mono.empty();
    }
}
