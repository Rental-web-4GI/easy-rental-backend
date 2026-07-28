package com.yowyob.easyrental.modules.conversation.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a single conversation message.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public record MessageDTO(
        UUID id,
        UUID conversationId,
        String senderType,
        UUID senderId,
        String body,
        Instant createdAt
) {
}
