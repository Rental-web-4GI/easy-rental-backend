package com.yowyob.easyrental.modules.conversation.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a conversation thread, from the viewer's perspective
 * (the {@code unread} count is the viewer's own unread count).
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public record ConversationDTO(
        UUID id,
        String type,
        String participantAType,
        UUID participantAId,
        String participantBType,
        UUID participantBId,
        String subject,
        int unread,
        Instant lastMessageAt
) {
}
