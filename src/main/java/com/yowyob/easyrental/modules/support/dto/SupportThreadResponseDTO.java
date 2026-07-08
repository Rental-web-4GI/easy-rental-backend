package com.yowyob.easyrental.modules.support.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SupportThreadResponseDTO(
        UUID id,
        String visitorEmail,
        String visitorName,
        String subject,
        String status,
        int adminUnreadCount,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt
) {}
