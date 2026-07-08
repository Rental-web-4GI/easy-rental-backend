package com.yowyob.easyrental.modules.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionResponseDTO(
    String planName,
    String description,
    BigDecimal price,
    Integer durationDays,
    Integer maxVehicles,
    Integer maxAgencies,
    LocalDateTime expiresAt,
    Boolean isExpired,
    Boolean autoRenew,
    Long daysRemaining,
    Boolean renewalDueSoon,
    Boolean overQuotaAgencies,
    Boolean overQuotaVehicles,
    Boolean overQuotaDrivers,
    Boolean overQuotaUsers,
    BigDecimal monthlyEquivalentPrice,
    String billingPeriod
) {}
