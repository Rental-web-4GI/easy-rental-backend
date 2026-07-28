package com.yowyob.easyrental.modules.tracking.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Outgoing representation of a recorded rental position.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record PositionResponseDTO(
        UUID id,
        UUID rentalId,
        UUID vehicleId,
        Double latitude,
        Double longitude,
        Instant recordedAt,
        String source
) {
}
