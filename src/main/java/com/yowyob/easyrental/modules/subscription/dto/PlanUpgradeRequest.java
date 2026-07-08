package com.yowyob.easyrental.modules.subscription.dto;

import com.yowyob.easyrental.shared.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;

public record PlanUpgradeRequest(
        @NotBlank String newPlan,
        PaymentMethod paymentMethod
) {}
