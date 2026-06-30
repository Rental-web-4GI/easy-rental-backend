package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.shared.enums.RentalType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RentalDurationCalculatorTest {

    @Test
    void shouldBillTwoDaysWhenRemainderWithinGrace() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 3, 18, 0);
        assertEquals(2L, RentalDurationCalculator.billableDays(start, end));
    }

    @Test
    void shouldBillThreeDaysWhenRemainderExceedsGrace() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 3, 21, 0);
        assertEquals(3L, RentalDurationCalculator.billableDays(start, end));
    }

    @Test
    void shouldBillHourlyWithCeil() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 12, 30);
        assertEquals(3L, RentalDurationCalculator.billableHours(start, end));
    }

    @Test
    void shouldResolveMonthlyUnits() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 10, 0, 0);
        assertEquals(2L, RentalDurationCalculator.billableMonths(start, end));
        assertEquals(2L, RentalDurationCalculator.billableUnits(start, end, RentalType.MONTHLY));
    }
}
