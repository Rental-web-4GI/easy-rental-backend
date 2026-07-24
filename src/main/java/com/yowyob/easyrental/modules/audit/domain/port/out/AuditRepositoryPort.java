package com.yowyob.easyrental.modules.audit.domain.port.out;

import com.yowyob.easyrental.modules.audit.domain.AuditEventEntity;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for audit event persistence.
 */
public interface AuditRepositoryPort {

    Mono<AuditEventEntity> save(AuditEventEntity entity);

    Flux<AuditEventEntity> search(UUID userId, String action, Instant from, Instant to, int limit, int offset);
}
