package com.yowyob.easyrental.modules.support.dto;

import java.util.UUID;

public record SupportMessageRequest(
        UUID threadId,
        String email,
        String visitorSessionId,
        String authorName,
        String visitorRole,
        String body
) {}
