package com.yowyob.easyrental.modules.poste.dto;

import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import java.util.List;
import java.util.UUID;

public record PosteResponseDTO(
    UUID id,
    String name,
    String description,
    List<PermissionEntity> permissions
) {}
