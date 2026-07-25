package com.yowyob.easyrental.modules.tracking.dto;

import java.util.List;

/**
 * Aggregated tracking summary for a rental: all recorded positions plus the
 * cumulative distance and the source of that computation ("GPS" when at
 * least two positions are available, "ODOMETER" as a frontend fallback
 * otherwise).
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record TrackingSummaryDTO(
        List<PositionResponseDTO> positions,
        Double trackedKm,
        String source
) {
}
