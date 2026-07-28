package com.yowyob.easyrental.modules.rating.dto;

import java.util.Map;

/**
 * Aggregated rating statistics for a given target (agency or client).
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record RatingStatsDTO(
        Double average,
        long count,
        Map<Integer, Long> distribution
) {
}
