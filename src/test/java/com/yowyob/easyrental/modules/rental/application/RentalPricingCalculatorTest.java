package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.rental.dto.RentalPricingBreakdown;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RentalPricingCalculatorTest {

    @Test
    void computeBreakdown_standardCase() {
        RentalPricingBreakdown breakdown = RentalPricingCalculator.computeBreakdown(
                new BigDecimal("100000"), new BigDecimal("0.05"), 30.0);

        assertThat(breakdown.rentalAmount()).isEqualByComparingTo("105000");
        assertThat(breakdown.cautionAmount()).isEqualByComparingTo("30000");
        assertThat(breakdown.commissionAmount()).isEqualByComparingTo("5000");
        assertThat(breakdown.totalDue()).isEqualByComparingTo("135000");
        assertThat(breakdown.requestedUpfront()).isEqualByComparingTo("81000");
    }

    @Test
    void computeBreakdown_zeroCommission_zeroDeposit() {
        RentalPricingBreakdown breakdown = RentalPricingCalculator.computeBreakdown(
                new BigDecimal("50000"), BigDecimal.ZERO, 0.0);

        assertThat(breakdown.rentalAmount()).isEqualByComparingTo("50000");
        assertThat(breakdown.cautionAmount()).isEqualByComparingTo("0");
        assertThat(breakdown.totalDue()).isEqualByComparingTo("50000");
        assertThat(breakdown.requestedUpfront()).isEqualByComparingTo("30000");
    }

    @Test
    void computeBreakdown_zeroBase_allZero() {
        RentalPricingBreakdown breakdown = RentalPricingCalculator.computeBreakdown(
                BigDecimal.ZERO, BigDecimal.ZERO, 10.0);

        assertThat(breakdown.rentalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(breakdown.cautionAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(breakdown.commissionAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(breakdown.totalDue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(breakdown.requestedUpfront()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void computeBreakdown_nullDepositPct_uses10Percent() {
        RentalPricingBreakdown breakdown = RentalPricingCalculator.computeBreakdown(
                new BigDecimal("100000"), BigDecimal.ZERO, null);

        assertThat(breakdown.cautionAmount()).isEqualByComparingTo("10000");
    }
}
