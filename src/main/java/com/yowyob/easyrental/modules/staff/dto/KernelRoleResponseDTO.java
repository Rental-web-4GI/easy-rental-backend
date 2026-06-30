package com.yowyob.easyrental.modules.staff.dto;

import java.util.UUID;

/**
 * Kernel administration role exposed to the organisation console.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public record KernelRoleResponseDTO(
        UUID id,
        String name,
        String code
) {
}
