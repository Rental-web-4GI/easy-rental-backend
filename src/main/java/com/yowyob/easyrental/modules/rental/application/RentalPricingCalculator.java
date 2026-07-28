package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.rental.dto.RentalPricingBreakdown;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure static utility computing the R2 pricing breakdown (rental vs caution, escrow-aware).
 * Source of truth formula (see docs/superpowers/plans/2026-07-24-release-2-caution-inspection-tracking.md):
 * <pre>
 * rentalAmount     = base + commission
 * cautionAmount    = base * (agencyDepositPct / 100)
 * totalDue         = rentalAmount + cautionAmount
 * requestedUpfront = totalDue * 0.60
 * </pre>
 * No Spring dependency — kept as a pure calculator so it can be unit tested without a context.
 */
public final class RentalPricingCalculator {

    private RentalPricingCalculator() {
    }

    private static final BigDecimal SIXTY_PCT = new BigDecimal("0.60");
    private static final BigDecimal DEFAULT_DEPOSIT_PCT = new BigDecimal("10");

    public static RentalPricingBreakdown computeBreakdown(
            BigDecimal base,
            BigDecimal commissionRate,
            Double agencyDepositPct
    ) {
        BigDecimal safeBase = base == null ? BigDecimal.ZERO : base;
        BigDecimal safeCommRate = commissionRate == null ? BigDecimal.ZERO : commissionRate;
        BigDecimal depositPct = agencyDepositPct == null
                ? DEFAULT_DEPOSIT_PCT
                : BigDecimal.valueOf(agencyDepositPct);

        BigDecimal commission = safeBase.multiply(safeCommRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal rentalAmount = safeBase.add(commission).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cautionAmount = safeBase.multiply(depositPct)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalDue = rentalAmount.add(cautionAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal upfront = totalDue.multiply(SIXTY_PCT).setScale(2, RoundingMode.HALF_UP);

        return new RentalPricingBreakdown(rentalAmount, cautionAmount, commission, totalDue, upfront);
    }
}
