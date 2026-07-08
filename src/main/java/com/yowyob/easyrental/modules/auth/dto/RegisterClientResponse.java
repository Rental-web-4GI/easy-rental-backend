package com.yowyob.easyrental.modules.auth.dto;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;

/**
 * Client registration outcome (local DB + optional kernel email verification).
 *
 * @author Easy Rental Team
 * @since 2026-07-05
 */
public record RegisterClientResponse(
        UserEntity user,
        boolean emailVerificationRequired,
        String message
) {
}
