package com.yowyob.easyrental.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * MFA confirmation request for kernel login step 2.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public record MfaConfirmRequest(
        @NotBlank String mfaToken,
        @NotBlank String code
) {
}
