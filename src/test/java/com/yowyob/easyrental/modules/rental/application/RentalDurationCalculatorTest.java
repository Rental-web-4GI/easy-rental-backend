package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.shared.enums.RentalType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RentalDurationCalculatorTest {

    private PricingEntity pricing() {
        PricingEntity p = new PricingEntity();
        p.setPricePerHour(new BigDecimal("1000"));
        p.setPricePerDay(new BigDecimal("20000"));
        p.setPricePerMonth(new BigDecimal("400000"));
        return p;
    }

    private final LocalDateTime base = LocalDateTime.of(2026, 1, 1, 8, 0);

    @Test
    void cascade_daily_lessThan24h_isOneDayMinimum() {
        assertThat(RentalDurationCalculator.computeBaseAmount(
                base, base.plusHours(6), RentalType.DAILY, pricing(), null))
                .isEqualByComparingTo("20000");
    }

    @Test
    void cascade_daily_2d12h_graceAbsorbs() {
        assertThat(RentalDurationCalculator.computeBaseAmount(
                base, base.plusDays(2).plusHours(12), RentalType.DAILY, pricing(), null))
                .isEqualByComparingTo("40000");
    }

    @Test
    void cascade_daily_2d15h_billsExcessHours() {
        // 2*20000 + (15-12)*1000 = 43000
        assertThat(RentalDurationCalculator.computeBaseAmount(
                base, base.plusDays(2).plusHours(15), RentalType.DAILY, pricing(), null))
                .isEqualByComparingTo("43000");
    }

    @Test
    void cascade_monthly_lessThanOneMonth_isOneMonthMinimum() {
        assertThat(RentalDurationCalculator.computeBaseAmount(
                base, base.plusDays(10), RentalType.MONTHLY, pricing(), null))
                .isEqualByComparingTo("400000");
    }

    @Test
    void cascade_monthly_2m17d15h_fullCascade() {
        // 2*400000 + (17-15)*20000 + (15-12)*1000 = 843000
        LocalDateTime end = base.plusMonths(2).plusDays(17).plusHours(15);
        assertThat(RentalDurationCalculator.computeBaseAmount(
                base, end, RentalType.MONTHLY, pricing(), null))
                .isEqualByComparingTo("843000");
    }

    @Test
    void cascade_monthly_2m15d_noExtra() {
        LocalDateTime end = base.plusMonths(2).plusDays(15);
        assertThat(RentalDurationCalculator.computeBaseAmount(
                base, end, RentalType.MONTHLY, pricing(), null))
                .isEqualByComparingTo("800000");
    }

    @Test
    void cascade_invalidPeriod_returnsOneUnitMinimum() {
        assertThat(RentalDurationCalculator.computeBaseAmount(
                base, base, RentalType.DAILY, pricing(), null))
                .isEqualByComparingTo("20000");
    }

    @Test
    void cascade_driverPricingAddedPerUnit() {
        PricingEntity driver = new PricingEntity();
        driver.setPricePerDay(new BigDecimal("5000"));
        driver.setPricePerHour(new BigDecimal("500"));
        // (20000+5000)*2 + (1000+500)*(15-12) = 50000 + 4500 = 54500
        assertThat(RentalDurationCalculator.computeBaseAmount(
                base, base.plusDays(2).plusHours(15), RentalType.DAILY, pricing(), driver))
                .isEqualByComparingTo("54500");
    }

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
