package com.yowyob.easyrental.modules.support.dto;

import jakarta.validation.constraints.NotBlank;

public record SupportReplyRequest(
        @NotBlank String body
) {}
