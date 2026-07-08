package com.yowyob.easyrental.modules.support.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SupportConversationResponseDTO(
        String conversationKey,
        String displayLabel,
        String visitorEmail,
        String visitorSessionId,
        String visitorRole,
        int adminUnreadCount,
        LocalDateTime lastMessageAt,
        UUID canonicalThreadId
) {}
