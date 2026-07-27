package com.yowyob.easyrental.modules.audit.application;

import com.yowyob.easyrental.modules.audit.domain.AuditEventEntity;
import com.yowyob.easyrental.modules.audit.domain.port.in.AuditUseCase;
import com.yowyob.easyrental.modules.audit.domain.port.out.AuditRepositoryPort;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Audit log use case implementation.
 *
 * <p>{@link #record} NEVER propagates an error — audit failures must not
 * impact the business flow that triggered them.
 *
 * @author Easy Rental Team
 * @since 2026-07-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditUseCaseImpl implements AuditUseCase {

    private final AuditRepositoryPort auditRepository;

    @Override
    public Mono<Void> record(UUID userId, String action, String resourceType, UUID resourceId, String ip,
            String userAgent, String metadata) {
        // IP / user-agent résolus depuis le contexte Reactor (AuditContextFilter)
        // quand l'appelant ne les fournit pas (ex. LOGIN_*).
        return Mono.deferContextual(ctx -> {
            String effectiveIp = ip != null ? ip : ctx.getOrDefault("AUDIT_IP", null);
            String effectiveUa = userAgent != null ? userAgent : ctx.getOrDefault("AUDIT_UA", null);
            AuditEventEntity entity = AuditEventEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .ip(effectiveIp)
                    .userAgent(effectiveUa)
                    .metadata(metadata)
                    .createdAt(Instant.now())
                    .isNewRecord(true)
                    .build();
            return auditRepository.save(entity).then();
        }).onErrorResume(e -> {
            log.warn("audit failed", e);
            return Mono.empty();
        });
    }

    @Override
    public Flux<AuditEventEntity> search(UUID userId, String action, Instant from, Instant to, int page, int size) {
        int safeSize = size <= 0 ? 50 : size;
        int safePage = Math.max(page, 0);
        int offset = safePage * safeSize;
        return auditRepository.search(userId, action, from, to, safeSize, offset);
    }
}
