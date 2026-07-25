package com.yowyob.easyrental.modules.rating.dto;

import java.util.UUID;

/**
 * Payload for submitting a post-rental rating.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record RatingCreateRequest(
        UUID rentalId,
        String raterType,
        UUID raterId,
        String targetType,
        UUID targetId,
        Short stars,
        String comment
) {
}
