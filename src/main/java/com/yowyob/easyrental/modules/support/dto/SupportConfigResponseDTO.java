package com.yowyob.easyrental.modules.support.dto;

public record SupportConfigResponseDTO(
        String adminEmail,
        String helpUrl,
        String consoleUrl
) {}
