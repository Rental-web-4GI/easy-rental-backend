package com.yowyob.easyrental.modules.permission.dto;

import java.util.UUID;

public record PermissionResponseDTO(
        UUID id,
        String name,
        String description,
        String tag,
        String module
) {
}
