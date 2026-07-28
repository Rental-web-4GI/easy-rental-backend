package com.yowyob.easyrental.modules.inspection.application;

import com.yowyob.easyrental.modules.inspection.domain.InspectionItemEntity;
import com.yowyob.easyrental.modules.inspection.domain.InspectionType;
import com.yowyob.easyrental.modules.inspection.domain.ItemStatus;
import com.yowyob.easyrental.modules.inspection.domain.RentalInspectionEntity;
import com.yowyob.easyrental.modules.inspection.dto.InspectionComparisonResult;
import com.yowyob.easyrental.modules.inspection.dto.ItemDiff;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JUnit tests for {@link InspectionComparisonService} — no Spring
 * context, no mocks, plain entity builders.
 */
class InspectionComparisonServiceTest {

    private final InspectionComparisonService service = new InspectionComparisonService();

    private RentalInspectionEntity inspection(InspectionType type, Integer odometer, Short fuelLevel) {
        return RentalInspectionEntity.builder()
                .id(UUID.randomUUID())
                .rentalId(UUID.randomUUID())
                .type(type)
                .odometer(odometer)
                .fuelLevel(fuelLevel)
                .build();
    }

    private InspectionItemEntity item(String code, ItemStatus status) {
        return InspectionItemEntity.builder()
                .id(UUID.randomUUID())
                .itemCode(code)
                .status(status)
                .build();
    }

    @Test
    void okToBroken_producesNewDamage() {
        RentalInspectionEntity in = inspection(InspectionType.CHECK_IN, null, null);
        RentalInspectionEntity out = inspection(InspectionType.CHECK_OUT, null, null);

        InspectionComparisonResult result = service.compare(
                in, List.of(item("TIRES", ItemStatus.OK)),
                out, List.of(item("TIRES", ItemStatus.BROKEN)));

        assertEquals(1, result.newDamages().size());
        assertEquals("TIRES", result.newDamages().get(0).itemCode());
        assertEquals(ItemDiff.DiffCategory.NEW_DAMAGE, result.newDamages().get(0).category());
        assertTrue(result.preExisting().isEmpty());
        assertTrue(result.newMissing().isEmpty());
    }

    @Test
    void damagedToDamaged_producesPreExisting() {
        RentalInspectionEntity in = inspection(InspectionType.CHECK_IN, null, null);
        RentalInspectionEntity out = inspection(InspectionType.CHECK_OUT, null, null);

        InspectionComparisonResult result = service.compare(
                in, List.of(item("BUMPERS", ItemStatus.DAMAGED)),
                out, List.of(item("BUMPERS", ItemStatus.DAMAGED)));

        assertEquals(1, result.preExisting().size());
        assertEquals("BUMPERS", result.preExisting().get(0).itemCode());
        assertEquals(ItemDiff.DiffCategory.PRE_EXISTING, result.preExisting().get(0).category());
        assertTrue(result.newDamages().isEmpty());
        assertTrue(result.newMissing().isEmpty());
    }

    @Test
    void okToMissing_producesNewMissing() {
        RentalInspectionEntity in = inspection(InspectionType.CHECK_IN, null, null);
        RentalInspectionEntity out = inspection(InspectionType.CHECK_OUT, null, null);

        InspectionComparisonResult result = service.compare(
                in, List.of(item("SPARE_TIRE", ItemStatus.OK)),
                out, List.of(item("SPARE_TIRE", ItemStatus.MISSING)));

        assertEquals(1, result.newMissing().size());
        assertEquals("SPARE_TIRE", result.newMissing().get(0).itemCode());
        assertEquals(ItemDiff.DiffCategory.NEW_MISSING, result.newMissing().get(0).category());
        assertTrue(result.newDamages().isEmpty());
        assertTrue(result.preExisting().isEmpty());
    }

    @Test
    void sameStatusOnAll_producesEmpty() {
        RentalInspectionEntity in = inspection(InspectionType.CHECK_IN, null, null);
        RentalInspectionEntity out = inspection(InspectionType.CHECK_OUT, null, null);

        List<InspectionItemEntity> inItems = List.of(
                item("TIRES", ItemStatus.OK),
                item("BUMPERS", ItemStatus.OK),
                item("LIGHTS", ItemStatus.OK));
        List<InspectionItemEntity> outItems = List.of(
                item("TIRES", ItemStatus.OK),
                item("BUMPERS", ItemStatus.OK),
                item("LIGHTS", ItemStatus.OK));

        InspectionComparisonResult result = service.compare(in, inItems, out, outItems);

        assertTrue(result.newDamages().isEmpty());
        assertTrue(result.preExisting().isEmpty());
        assertTrue(result.newMissing().isEmpty());
    }

    @Test
    void fuelDelta_computedFromLevels() {
        RentalInspectionEntity in = inspection(InspectionType.CHECK_IN, null, (short) 6);
        RentalInspectionEntity out = inspection(InspectionType.CHECK_OUT, null, (short) 2);

        InspectionComparisonResult result = service.compare(in, List.of(), out, List.of());

        assertEquals(-4, result.fuelDelta());
    }

    @Test
    void kmTraveled_computedFromOdometers() {
        RentalInspectionEntity in = inspection(InspectionType.CHECK_IN, 50000, null);
        RentalInspectionEntity out = inspection(InspectionType.CHECK_OUT, 50350, null);

        InspectionComparisonResult result = service.compare(in, List.of(), out, List.of());

        assertEquals(350L, result.kmTraveled());
    }

    @Test
    void nullOdometerReturnsNullKm() {
        RentalInspectionEntity in = inspection(InspectionType.CHECK_IN, null, null);
        RentalInspectionEntity out = inspection(InspectionType.CHECK_OUT, 50350, null);

        InspectionComparisonResult result = service.compare(in, List.of(), out, List.of());

        assertNull(result.kmTraveled());
    }
}
