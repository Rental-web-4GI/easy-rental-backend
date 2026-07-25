package com.yowyob.easyrental.modules.rating.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a submitted rating.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record RatingResponseDTO(
        UUID id,
        UUID rentalId,
        String raterType,
        UUID raterId,
        String targetType,
        UUID targetId,
        Short stars,
        String comment,
        Instant createdAt
) {
}
