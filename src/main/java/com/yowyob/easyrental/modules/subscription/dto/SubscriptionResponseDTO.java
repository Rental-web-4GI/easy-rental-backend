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
    Boolean isExpired
) {}