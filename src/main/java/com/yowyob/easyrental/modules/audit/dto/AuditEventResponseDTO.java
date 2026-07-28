package com.yowyob.easyrental.modules.audit.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API response DTO mirroring {@link com.yowyob.easyrental.modules.audit.domain.AuditEventEntity}.
 *
 * @author Easy Rental Team
 * @since 2026-07-24
 */
public record AuditEventResponseDTO(
        UUID id,
        UUID userId,
        String action,
        String resourceType,
        UUID resourceId,
        String ip,
        String userAgent,
        String metadata,
        Instant createdAt
) {
}
