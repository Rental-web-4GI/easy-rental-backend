package com.yowyob.easyrental.modules.subscription.mapper;

import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    private static final int RENEWAL_WARNING_DAYS = 2;
    private static final int YEARLY_THRESHOLD_DAYS = 360;

    public SubscriptionResponseDTO toResponseDTO(OrganizationEntity org, SubscriptionPlanEntity plan) {
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = org.getSubscriptionExpiresAt() != null
                && org.getSubscriptionExpiresAt().isBefore(now);

        long daysRemaining = 0L;
        if (org.getSubscriptionExpiresAt() != null && !isExpired) {
            daysRemaining = Math.max(
                    0L,
                    ChronoUnit.DAYS.between(now.toLocalDate(), org.getSubscriptionExpiresAt().toLocalDate()));
        }

        boolean isFreePlan = "FREE".equalsIgnoreCase(plan.getName());
        boolean renewalDueSoon = !isFreePlan
                && org.getSubscriptionExpiresAt() != null
                && !isExpired
                && daysRemaining <= RENEWAL_WARNING_DAYS;

        int currentAgencies = safeCount(org.getCurrentAgencies());
        int currentVehicles = safeCount(org.getCurrentVehicles());
        int currentDrivers = safeCount(org.getCurrentDrivers());
        int currentUsers = safeCount(org.getCurrentUsers());

        return new SubscriptionResponseDTO(
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getDurationDays(),
                plan.getMaxVehicles(),
                plan.getMaxAgencies(),
                org.getSubscriptionExpiresAt(),
                isExpired,
                Boolean.FALSE,
                daysRemaining,
                renewalDueSoon,
                currentAgencies > safeQuota(plan.getMaxAgencies()),
                currentVehicles > safeQuota(plan.getMaxVehicles()),
                currentDrivers > safeQuota(plan.getMaxDrivers()),
                currentUsers > safeQuota(plan.getMaxUsers()),
                resolveMonthlyEquivalent(plan.getPrice(), plan.getDurationDays()),
                resolveBillingPeriod(plan.getDurationDays(), isFreePlan));
    }

    public static BigDecimal resolveMonthlyEquivalent(BigDecimal price, Integer durationDays) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        if (durationDays != null && durationDays >= YEARLY_THRESHOLD_DAYS) {
            return price.divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP);
        }
        return price;
    }

    public static String resolveBillingPeriod(Integer durationDays, boolean isFreePlan) {
        if (isFreePlan || durationDays == null || durationDays <= 0) {
            return "UNLIMITED";
        }
        if (durationDays >= YEARLY_THRESHOLD_DAYS) {
            return "YEARLY";
        }
        return "MONTHLY";
    }

    private int safeCount(Integer value) {
        return value != null ? value : 0;
    }

    private int safeQuota(Integer value) {
        return value != null ? value : 0;
    }
}
