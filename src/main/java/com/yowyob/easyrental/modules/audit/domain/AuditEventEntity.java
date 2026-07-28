package com.yowyob.easyrental.modules.audit.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC entity mapped on the {@code audit_events} table (created in Task 1's
 * Liquibase changelog {@code 24-audit-events.xml}).
 *
 * @author Easy Rental Team
 * @since 2026-07-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_events")
public class AuditEventEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("action")
    private String action;

    @Column("resource_type")
    private String resourceType;

    @Column("resource_id")
    private UUID resourceId;

    @Column("ip")
    private String ip;

    @Column("user_agent")
    private String userAgent;

    /**
     * Raw JSON string (kept simple in R1 — no Jackson (de)serialization).
     */
    @Column("metadata")
    private String metadata;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
