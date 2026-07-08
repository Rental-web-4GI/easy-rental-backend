package com.yowyob.easyrental.modules.support.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SupportMessageResponseDTO(
        UUID id,
        UUID threadId,
        String senderRole,
        String body,
        LocalDateTime createdAt
) {}
