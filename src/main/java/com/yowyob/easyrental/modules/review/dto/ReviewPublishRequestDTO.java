package com.yowyob.easyrental.modules.review.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewPublishRequestDTO(
    @NotNull Boolean published
) {}
