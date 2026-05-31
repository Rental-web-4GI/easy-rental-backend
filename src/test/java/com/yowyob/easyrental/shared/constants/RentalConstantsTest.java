package com.yowyob.easyrental.shared.constants;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RentalConstantsTest {

    @Test
    void shouldExposeDepositRate() {
        assertEquals(new BigDecimal("0.10"), RentalConstants.DEPOSIT_RATE);
    }

    @Test
    void shouldExposePenaltyRate() {
        assertEquals(new BigDecimal("0.05"), RentalConstants.PENALTY_RATE);
    }

    @Test
    void shouldExposeReservationDepositRate() {
        assertEquals(new BigDecimal("0.60"), RentalConstants.RESERVATION_DEPOSIT_RATE);
    }

    @Test
    void shouldExposePlatformCommissionRate() {
        assertEquals(new BigDecimal("0.01"), RentalConstants.PLATFORM_COMMISSION_RATE);
    }

    @Test
    void shouldExposeMaintenanceHoursAfterReturn() {
        assertEquals(24, RentalConstants.MAINTENANCE_HOURS_AFTER_RETURN);
    }
}
