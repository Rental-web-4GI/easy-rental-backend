package com.yowyob.easyrental.shared.constants;

import java.math.BigDecimal;

/**
 * Business constants for rental operations.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public final class RentalConstants {

    public static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.10");
    public static final BigDecimal PENALTY_RATE = new BigDecimal("0.05");
    public static final BigDecimal RESERVATION_DEPOSIT_RATE = new BigDecimal("0.60");
    public static final BigDecimal PLATFORM_COMMISSION_RATE = new BigDecimal("0.01");
    public static final int MAINTENANCE_HOURS_AFTER_RETURN = 24;

    private RentalConstants() {
    }
}
