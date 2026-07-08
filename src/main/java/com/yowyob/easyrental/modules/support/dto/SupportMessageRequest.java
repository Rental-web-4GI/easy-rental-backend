package com.yowyob.easyrental.modules.support.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record SupportMessageRequest(
        UUID threadId,
        @NotBlank @Email String email,
        String authorName,
        @NotBlank String body
) {}
