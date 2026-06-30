package com.yowyob.easyrental.modules.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pour répondre avec une notification.
 * Les états de lecture agence et organisation sont indépendants.
 */
public record NotificationResponseDTO(
        UUID id,
        UUID locationId,
        UUID resourceId,
        String resourceType,
        String reason,
        UUID vehicleId,
        UUID driverId,
        LocalDateTime createdAt,
        Boolean isReadAgency,
        Boolean isReadOrg,
        String details
) {}
