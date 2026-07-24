package com.yowyob.easyrental.modules.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ligne de facturation admin — abonnement actif d'une organisation,
 * enrichi avec le nom d'org, le plan et le prix mensuel du plan.
 *
 * @author Easy Rental Team
 * @since 2026-07-24
 */
public record SubscriptionBillingDTO(
        UUID subscriptionId,
        UUID organizationId,
        String organizationName,
        String organizationEmail,
        String accountType,
        String planName,
        BigDecimal price,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate
) {}
