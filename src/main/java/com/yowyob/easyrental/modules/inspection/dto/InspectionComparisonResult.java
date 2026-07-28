package com.yowyob.easyrental.modules.inspection.dto;

import java.util.List;

/**
 * Result of comparing a rental's CHECK_IN inspection against its CHECK_OUT
 * inspection: item-level diffs plus fuel and mileage deltas.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record InspectionComparisonResult(
        List<ItemDiff> newDamages,     // items where category = NEW_DAMAGE
        List<ItemDiff> preExisting,    // category = PRE_EXISTING
        List<ItemDiff> newMissing,     // category = NEW_MISSING
        Integer fuelDelta,             // fuelOut - fuelIn (can be negative)
        Long kmTraveled                // odometerOut - odometerIn (long)
) {}
