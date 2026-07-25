package com.yowyob.easyrental.modules.inspection.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API response representation of a rental inspection with its checklist items.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record InspectionResponseDTO(
        UUID id,
        UUID rentalId,
        String type,
        Integer odometer,
        Short fuelLevel,
        String notes,
        List<String> photoUrls,
        UUID performedBy,
        Instant performedAt,
        List<InspectionItemDTO> items
) {
}
