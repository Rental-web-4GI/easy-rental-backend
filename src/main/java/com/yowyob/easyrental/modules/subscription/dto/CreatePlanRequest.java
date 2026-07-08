package com.yowyob.easyrental.modules.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreatePlanRequest(
        @NotBlank String name,
        String description,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotNull @PositiveOrZero Integer durationDays,
        @PositiveOrZero Integer maxVehicles,
        @PositiveOrZero Integer maxDrivers,
        @PositiveOrZero Integer maxAgencies,
        @PositiveOrZero Integer maxUsers,
        Boolean hasGeofencing,
        Boolean hasChat
) {}
