package com.yowyob.easyrental.kernel.infrastructure.dto;

/**
 * Result of a kernel login attempt.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public record KernelLoginResult(
        boolean mfaRequired,
        String accessToken,
        String mfaToken,
        String mfaChannel
) {

    public static KernelLoginResult authenticated(String accessToken) {
        return new KernelLoginResult(false, accessToken, null, null);
    }

    public static KernelLoginResult mfaRequired(String mfaToken, String channel) {
        return new KernelLoginResult(true, null, mfaToken, channel);
    }
}
