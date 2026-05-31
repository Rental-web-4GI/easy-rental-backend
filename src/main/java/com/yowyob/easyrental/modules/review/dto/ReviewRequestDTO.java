package com.yowyob.easyrental.modules.review.dto;

import com.yowyob.easyrental.shared.enums.ResourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReviewRequestDTO(
    @NotNull UUID resourceId,
    @NotNull ResourceType resourceType,
    @NotNull @Min(1) @Max(5) Integer rating,
    String comment,
    String authorName // on prend le nom du user connecté
) {}
