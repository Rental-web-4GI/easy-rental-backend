package com.yowyob.easyrental.modules.auth.dto;

/**
 * Authentication response for login and refresh flows.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public record AuthResponse(
        String token,
        Boolean mfaRequired,
        String mfaToken,
        String nextStep,
        String mfaChannel
) {

    public AuthResponse(String token) {
        this(token, false, null, null, null);
    }

    public static AuthResponse withToken(String token) {
        return new AuthResponse(token, false, null, null, null);
    }

    public static AuthResponse mfaRequired(String mfaToken, String channel) {
        return new AuthResponse(null, true, mfaToken, "CONFIRM_MFA", channel);
    }
}
