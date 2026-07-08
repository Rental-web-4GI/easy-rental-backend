package com.yowyob.easyrental.modules.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignPlanRequest(
        @NotBlank String planName
) {}
