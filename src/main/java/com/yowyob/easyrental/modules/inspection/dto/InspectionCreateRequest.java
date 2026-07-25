package com.yowyob.easyrental.modules.inspection.dto;

import java.util.List;

/**
 * Request body to create a rental inspection (CHECK_IN or CHECK_OUT).
 * At least 4 photos are required. If {@code items} is null or empty, the
 * default checklist ({@link com.yowyob.easyrental.modules.inspection.domain.InspectionChecklistDefaults})
 * is seeded with status OK.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record InspectionCreateRequest(
        String type,
        Integer odometer,
        Short fuelLevel,
        String notes,
        List<String> photoUrls,
        List<InspectionItemDTO> items
) {
}
