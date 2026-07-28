package com.yowyob.easyrental.modules.inspection.application;

import com.yowyob.easyrental.modules.inspection.domain.InspectionItemEntity;
import com.yowyob.easyrental.modules.inspection.domain.ItemStatus;
import com.yowyob.easyrental.modules.inspection.domain.RentalInspectionEntity;
import com.yowyob.easyrental.modules.inspection.dto.InspectionComparisonResult;
import com.yowyob.easyrental.modules.inspection.dto.ItemDiff;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure domain service comparing a rental's CHECK_IN inspection against its
 * CHECK_OUT inspection: item-level diffs (new damage, pre-existing damage,
 * new missing items) plus fuel and mileage deltas.
 *
 * <p>No DB access — input entities/items are already loaded by the caller.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Service
public class InspectionComparisonService {

    /**
     * Compare an CHECK_IN inspection to a CHECK_OUT inspection.
     * Pure logic — no DB access. Input items already loaded.
     *
     * Rules:
     *  - IN=OK/N/A  → OUT=BROKEN|DAMAGED  → NEW_DAMAGE
     *  - IN=DAMAGED → OUT=DAMAGED|BROKEN  → PRE_EXISTING
     *  - IN=OK      → OUT=MISSING         → NEW_MISSING
     *  - IN=BROKEN|DAMAGED|MISSING → OUT=OK → RESOLVED (e.g. réparé pendant la location)
     *  - IN=X       → OUT=X (identical)   → UNCHANGED (omitted from returned lists)
     *
     * Missing item in OUT (item present in IN but absent in OUT) counts as NEW_MISSING
     * only if the IN status was OK. Otherwise ignored (pre-existing damage stays as-is).
     *
     * fuelDelta = fuelOut - fuelIn (both may be null → returns null delta).
     * kmTraveled = max(0, odometerOut - odometerIn); null if either is null.
     */
    public InspectionComparisonResult compare(
            RentalInspectionEntity checkIn,
            List<InspectionItemEntity> checkInItems,
            RentalInspectionEntity checkOut,
            List<InspectionItemEntity> checkOutItems
    ) {
        Map<String, ItemStatus> inStatus = new HashMap<>();
        Map<String, ItemStatus> outStatus = new HashMap<>();
        Map<String, String> outNotes = new HashMap<>();

        if (checkInItems != null) {
            for (InspectionItemEntity it : checkInItems) {
                inStatus.put(it.getItemCode(), it.getStatus());
            }
        }
        if (checkOutItems != null) {
            for (InspectionItemEntity it : checkOutItems) {
                outStatus.put(it.getItemCode(), it.getStatus());
                outNotes.put(it.getItemCode(), it.getNote());
            }
        }

        List<ItemDiff> newDamages = new ArrayList<>();
        List<ItemDiff> preExisting = new ArrayList<>();
        List<ItemDiff> newMissing = new ArrayList<>();

        Set<String> allCodes = new HashSet<>();
        allCodes.addAll(inStatus.keySet());
        allCodes.addAll(outStatus.keySet());

        for (String code : allCodes) {
            ItemStatus before = inStatus.get(code);
            ItemStatus after = outStatus.get(code);
            String note = outNotes.get(code);
            String bName = before == null ? null : before.name();
            String aName = after == null ? null : after.name();

            // OUT missing entirely for this code
            if (after == null) {
                if (before == ItemStatus.OK) {
                    newMissing.add(new ItemDiff(code, bName, null, note, ItemDiff.DiffCategory.NEW_MISSING));
                }
                continue;
            }
            // IN missing entirely (item newly appearing in OUT) — ignore for now
            if (before == null) {
                continue;
            }

            // Identical status is only truly UNCHANGED for OK/NOT_APPLICABLE/MISSING —
            // DAMAGED==DAMAGED and BROKEN==BROKEN must still be classified as
            // PRE_EXISTING further below, so we do NOT short-circuit on equality here.

            // OK → MISSING = new missing
            if (before == ItemStatus.OK && after == ItemStatus.MISSING) {
                newMissing.add(new ItemDiff(code, bName, aName, note, ItemDiff.DiffCategory.NEW_MISSING));
                continue;
            }

            // OK/NOT_APPLICABLE → DAMAGED/BROKEN = new damage
            if ((before == ItemStatus.OK || before == ItemStatus.NOT_APPLICABLE)
                    && (after == ItemStatus.DAMAGED || after == ItemStatus.BROKEN)) {
                newDamages.add(new ItemDiff(code, bName, aName, note, ItemDiff.DiffCategory.NEW_DAMAGE));
                continue;
            }

            // DAMAGED → DAMAGED/BROKEN = pre-existing
            if (before == ItemStatus.DAMAGED && (after == ItemStatus.DAMAGED || after == ItemStatus.BROKEN)) {
                preExisting.add(new ItemDiff(code, bName, aName, note, ItemDiff.DiffCategory.PRE_EXISTING));
                continue;
            }

            // BROKEN → BROKEN = pre-existing
            if (before == ItemStatus.BROKEN && after == ItemStatus.BROKEN) {
                preExisting.add(new ItemDiff(code, bName, aName, note, ItemDiff.DiffCategory.PRE_EXISTING));
                continue;
            }

            // MISSING → OK = resolved (silent)
            // Other combinations (e.g. BROKEN → OK) = resolved — omitted from result lists
        }

        Integer fuelDelta = null;
        if (checkIn.getFuelLevel() != null && checkOut.getFuelLevel() != null) {
            fuelDelta = checkOut.getFuelLevel() - checkIn.getFuelLevel();
        }

        Long kmTraveled = null;
        if (checkIn.getOdometer() != null && checkOut.getOdometer() != null) {
            long diff = (long) checkOut.getOdometer() - (long) checkIn.getOdometer();
            kmTraveled = Math.max(0L, diff);
        }

        return new InspectionComparisonResult(newDamages, preExisting, newMissing, fuelDelta, kmTraveled);
    }
}
