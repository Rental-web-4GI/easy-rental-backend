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

    /**
     * Montant de base facturé selon le modèle en cascade R2 :
     * <ul>
     *   <li>HORAIRE : heures (arrondi sup, min 1) × tarif heure.</li>
     *   <li>JOUR : jours pleins × tarif jour + heures excédant la grâce de 12h × tarif heure.
     *       Si &lt; 24h, minimum 1 jour (pas d'excédent horaire).</li>
     *   <li>MOIS : mois pleins × tarif mois + jours excédant la grâce de 15j × tarif jour
     *       + heures excédant 12h × tarif heure. Si &lt; 1 mois plein, minimum 1 mois.</li>
     * </ul>
     * Les tarifs véhicule + chauffeur sont additionnés par unité.
     * Période nulle/inversée → 1 unité minimum de l'unité choisie.
     */
    public static BigDecimal computeBaseAmount(LocalDateTime start, LocalDateTime end,
            RentalType type, PricingEntity vehicle, PricingEntity driver) {
        BigDecimal hourRate = unitPrice(vehicle, RentalType.HOURLY).add(unitPrice(driver, RentalType.HOURLY));
        BigDecimal dayRate = unitPrice(vehicle, RentalType.DAILY).add(unitPrice(driver, RentalType.DAILY));
        BigDecimal monthRate = unitPrice(vehicle, RentalType.MONTHLY).add(unitPrice(driver, RentalType.MONTHLY));

        if (end == null || start == null || !end.isAfter(start)) {
            // période invalide → 1 unité minimum
            return switch (type) {
                case HOURLY -> hourRate;
                case DAILY -> dayRate;
                case MONTHLY -> monthRate;
            };
        }

        return switch (type) {
            case HOURLY -> hourRate.multiply(BigDecimal.valueOf(billableHours(start, end)));
            case DAILY -> dailyCascade(start, end, hourRate, dayRate);
            case MONTHLY -> monthlyCascade(start, end, hourRate, dayRate, monthRate);
        };
    }

    private static BigDecimal dailyCascade(LocalDateTime start, LocalDateTime end,
            BigDecimal hourRate, BigDecimal dayRate) {
        Duration d = Duration.between(start, end);
        long fullDays = d.toDays();
        if (fullDays == 0) {
            return dayRate; // minimum 1 jour
        }
        long remHours = d.minusDays(fullDays).toHours();
        long extraHours = remHours > DAILY_GRACE_HOURS ? remHours - DAILY_GRACE_HOURS : 0L;
        return dayRate.multiply(BigDecimal.valueOf(fullDays))
                .add(hourRate.multiply(BigDecimal.valueOf(extraHours)));
    }

    private static BigDecimal monthlyCascade(LocalDateTime start, LocalDateTime end,
            BigDecimal hourRate, BigDecimal dayRate, BigDecimal monthRate) {
        long fullMonths = fullCalendarMonths(start, end);
        if (fullMonths == 0) {
            return monthRate; // minimum 1 mois
        }
        LocalDateTime anchor = start.plusMonths(fullMonths);
        Duration rem = Duration.between(anchor, end);
        long remDays = rem.toDays();
        long remHours = rem.minusDays(remDays).toHours();
        long extraDays = remDays > MONTHLY_GRACE_DAYS ? remDays - MONTHLY_GRACE_DAYS : 0L;
        long extraHours = remHours > DAILY_GRACE_HOURS ? remHours - DAILY_GRACE_HOURS : 0L;
        return monthRate.multiply(BigDecimal.valueOf(fullMonths))
                .add(dayRate.multiply(BigDecimal.valueOf(extraDays)))
                .add(hourRate.multiply(BigDecimal.valueOf(extraHours)));
    }

    /** Nombre de mois calendaires complets entre start et end. */
    public static long fullCalendarMonths(LocalDateTime start, LocalDateTime end) {
        long months = 0L;
        while (!start.plusMonths(months + 1).isAfter(end)) {
            months++;
        }
        return months;
    }
}
