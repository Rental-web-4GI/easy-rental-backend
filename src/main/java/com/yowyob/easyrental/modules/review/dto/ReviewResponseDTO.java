package com.yowyob.easyrental.modules.review.dto;

import com.yowyob.easyrental.shared.enums.ResourceType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponseDTO(
    UUID id,
    UUID resourceId,
    ResourceType resourceType,
    Integer rating,
    String comment,
    String authorName,
    LocalDateTime createdAt
) {}
