package com.yowyob.easyrental.modules.audit.domain.port.in;

import com.yowyob.easyrental.modules.audit.domain.AuditEventEntity;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for audit logging use cases.
 *
 * <p>{@link #record} is meant to be called fire-and-forget from business flows
 * (auth, org, etc.) — it must never propagate an error that could interrupt
 * the caller's reactive chain.
 *
 * @author Easy Rental Team
 * @since 2026-07-24
 */
public interface AuditUseCase {

    Mono<Void> record(UUID userId, String action, String resourceType, UUID resourceId, String ip,
            String userAgent, String metadata);

    Flux<AuditEventEntity> search(UUID userId, String action, Instant from, Instant to, int page, int size);
}
