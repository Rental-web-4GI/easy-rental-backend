package com.yowyob.easyrental.modules.support.dto;

import java.util.UUID;

public record SupportSendResponseDTO(
        UUID threadId,
        String confirmationMessage
) {}
