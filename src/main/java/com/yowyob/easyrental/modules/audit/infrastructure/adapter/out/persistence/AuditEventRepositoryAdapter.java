package com.yowyob.easyrental.modules.audit.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.audit.domain.AuditEventEntity;
import com.yowyob.easyrental.modules.audit.domain.port.out.AuditRepositoryPort;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuditEventRepositoryAdapter implements AuditRepositoryPort {

    private final AuditEventRepository auditEventRepository;

    @Override
    public Mono<AuditEventEntity> save(AuditEventEntity entity) {
        return auditEventRepository.save(entity);
    }

    @Override
    public Flux<AuditEventEntity> search(UUID userId, String action, Instant from, Instant to, int limit, int offset) {
        return auditEventRepository.search(userId, action, from, to, limit, offset);
    }
}
