package com.yowyob.easyrental.modules.inspection.dto;

/**
 * Diff of a single checklist item between a CHECK_IN and a CHECK_OUT
 * inspection of the same rental.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record ItemDiff(
        String itemCode,
        String statusBefore,   // ItemStatus.name() or null
        String statusAfter,    // ItemStatus.name() or null
        String noteAfter,      // may be null
        DiffCategory category
) {
    public enum DiffCategory { NEW_DAMAGE, PRE_EXISTING, NEW_MISSING, RESOLVED, UNCHANGED }
}
