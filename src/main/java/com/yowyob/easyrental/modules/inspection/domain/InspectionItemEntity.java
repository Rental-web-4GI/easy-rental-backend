package com.yowyob.easyrental.modules.inspection.domain;

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
 * R2DBC entity mapped on the {@code inspection_items} table (created in
 * Task 1's Liquibase changelog {@code 32-inspection-items.xml}).
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("inspection_items")
public class InspectionItemEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID inspectionId;
    private String itemCode;
    private ItemStatus status;
    private String note;
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
