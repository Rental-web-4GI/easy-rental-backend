package com.yowyob.easyrental.modules.loyalty.domain;

/**
 * Loyalty tier derived from annual (last 12 months) positive point deltas.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public enum LoyaltyTier {
    BRONZE, ARGENT, OR, PLATINE;

    public static LoyaltyTier fromAnnualPoints(int annual) {
        if (annual >= 5000) {
            return PLATINE;
        }
        if (annual >= 2000) {
            return OR;
        }
        if (annual >= 500) {
            return ARGENT;
        }
        return BRONZE;
    }
}
