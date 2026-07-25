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
import java.util.List;
import java.util.UUID;

/**
 * R2DBC entity mapped on the {@code rental_inspections} table (created in
 * Task 1's Liquibase changelog {@code 31-inspections.xml}).
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("rental_inspections")
public class RentalInspectionEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID rentalId;
    private InspectionType type;
    private Integer odometer;
    private Short fuelLevel;
    private String notes;
    private List<String> photoUrls;
    private UUID performedBy;
    private Instant performedAt;

    /**
     * Checklist items belonging to this inspection. Not a DB column — loaded
     * separately via {@code InspectionItemRepositoryPort}.
     */
    @Transient
    private List<InspectionItemEntity> items;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
