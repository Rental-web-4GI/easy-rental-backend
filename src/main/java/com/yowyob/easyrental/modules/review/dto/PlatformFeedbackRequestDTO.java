package com.yowyob.easyrental.modules.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlatformFeedbackRequestDTO(
    @NotBlank @Size(max = 255) String authorName,
    @NotBlank @Size(max = 50) String authorRole,
    @NotNull @Min(1) @Max(5) Integer rating,
    @Size(max = 2000) String comment
) {}
