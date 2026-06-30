package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.shared.enums.RentalType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;

/**
 * Computes billable rental units with grace thresholds (12h daily, 15d monthly).
 */
public final class RentalDurationCalculator {

    public static final int DAILY_GRACE_HOURS = 12;
    public static final int MONTHLY_GRACE_DAYS = 15;

    private RentalDurationCalculator() {
    }

    public static long billableUnits(LocalDateTime start, LocalDateTime end, RentalType rentalType) {
        if (end.isBefore(start) || end.isEqual(start)) {
            return 1L;
        }
        return switch (rentalType) {
            case HOURLY -> billableHours(start, end);
            case MONTHLY -> billableMonths(start, end);
            case DAILY -> billableDays(start, end);
        };
    }

    public static long billableHours(LocalDateTime start, LocalDateTime end) {
        long hours = Duration.between(start, end).toHours();
        long remainderMinutes = Duration.between(start, end).toMinutes() % 60;
        long billed = hours + (remainderMinutes > 0 ? 1 : 0);
        return Math.max(1L, billed);
    }

    public static long billableDays(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        long fullDays = duration.toDays();
        long remainderHours = duration.minusDays(fullDays).toHours();
        long extraDay = remainderHours > DAILY_GRACE_HOURS ? 1L : 0L;
        return Math.max(1L, fullDays + extraDay);
    }

    public static long billableMonths(LocalDateTime start, LocalDateTime end) {
        int yearDiff = end.getYear() - start.getYear();
        int monthDiff = end.getMonthValue() - start.getMonthValue();
        int dayDiff = end.getDayOfMonth() - start.getDayOfMonth();
        long months = (long) yearDiff * 12 + monthDiff;
        if (dayDiff > MONTHLY_GRACE_DAYS) {
            months += 1;
        } else if (dayDiff < 0 && Math.abs(dayDiff) > MONTHLY_GRACE_DAYS) {
            months += 1;
        }
        return Math.max(1L, months);
    }

    public static BigDecimal unitPrice(PricingEntity pricing, RentalType rentalType) {
        if (pricing == null) {
            return BigDecimal.ZERO;
        }
        return switch (rentalType) {
            case HOURLY -> pricing.getPricePerHour() != null ? pricing.getPricePerHour() : BigDecimal.ZERO;
            case MONTHLY -> pricing.getPricePerMonth() != null ? pricing.getPricePerMonth() : BigDecimal.ZERO;
            case DAILY -> pricing.getPricePerDay() != null ? pricing.getPricePerDay() : BigDecimal.ZERO;
        };
    }
}
