package com.yowyob.easyrental.modules.inspection.domain;

import java.util.List;

/**
 * Default checklist item codes seeded on an inspection when the caller does
 * not supply a custom list of items.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public final class InspectionChecklistDefaults {

    private InspectionChecklistDefaults() {
    }

    public static final List<String> ITEM_CODES = List.of(
            "TIRES", "SPARE_TIRE", "TRIANGLE_JACK", "HEADLIGHTS", "TURN_SIGNALS",
            "BUMPERS", "DOORS", "WINDOWS_MIRRORS", "INTERIOR", "AC", "RADIO", "DOCUMENTS"
    );
}
