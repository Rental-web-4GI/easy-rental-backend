package com.yowyob.easyrental.modules.audit.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.audit.domain.AuditEventEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * R2DBC repository for the {@code audit_events} table.
 *
 * <p>Named {@code AuditEventRepository} (rather than {@code AuditRepository})
 * to avoid a name collision with the pre-existing, unrelated
 * {@code com.yowyob.easyrental.modules.audit.infrastructure.adapter.out.persistence.AuditRepository}
 * which maps the legacy {@code audits} table (see {@code AuditEntity} /
 * {@code AuditListener}).
 */
@Repository
public interface AuditEventRepository extends R2dbcRepository<AuditEventEntity, UUID> {

    @Query("SELECT * FROM audit_events "
            + "WHERE (:userId IS NULL OR user_id = :userId) "
            + "AND (:action IS NULL OR action = :action) "
            + "AND (:fromTs IS NULL OR created_at >= :fromTs) "
            + "AND (:toTs IS NULL OR created_at <= :toTs) "
            + "ORDER BY created_at DESC "
            + "LIMIT :limit OFFSET :offset")
    Flux<AuditEventEntity> search(UUID userId, String action, Instant fromTs, Instant toTs, int limit, int offset);
}
