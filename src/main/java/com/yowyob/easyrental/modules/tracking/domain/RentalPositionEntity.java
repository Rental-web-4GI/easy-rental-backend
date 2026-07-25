package com.yowyob.easyrental.modules.tracking.domain;

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
 * R2DBC entity mapped on the {@code rental_positions} table (created in
 * Task 1's Liquibase changelog). The PostGIS {@code location} GEOGRAPHY
 * column is intentionally NOT mapped here: it is nullable and left NULL on
 * INSERT, reserved for future geofence queries (R3).
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("rental_positions")
public class RentalPositionEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID rentalId;
    private UUID vehicleId;
    private Double latitude;
    private Double longitude;
    private Instant recordedAt;
    private PositionSource source;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
