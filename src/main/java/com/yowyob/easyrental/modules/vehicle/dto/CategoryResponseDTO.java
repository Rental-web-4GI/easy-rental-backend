package com.yowyob.easyrental.modules.vehicle.dto;

import java.util.UUID;

public record CategoryResponseDTO(
        UUID id,
        UUID organizationId,
        String name,
        String description
) {
}
