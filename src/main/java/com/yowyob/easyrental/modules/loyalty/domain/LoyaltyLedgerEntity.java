package com.yowyob.easyrental.modules.loyalty.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC entity mapped on the {@code loyalty_ledger} table (migration from R1).
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("loyalty_ledger")
public class LoyaltyLedgerEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID clientId;
    private Integer deltaPoints;
    private String sourceType;
    private UUID sourceId;
    private Integer balanceAfter;
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
