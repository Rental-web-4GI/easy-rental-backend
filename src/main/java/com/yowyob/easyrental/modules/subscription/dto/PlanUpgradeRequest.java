package com.yowyob.easyrental.modules.subscription.dto;

import com.yowyob.easyrental.modules.subscription.domain.PlanType;
import jakarta.validation.constraints.NotNull;

// Utilise l'Enum PlanType (FREE, PRO, ENTERPRISE)
public record PlanUpgradeRequest(
    @NotNull PlanType newPlan
) {}