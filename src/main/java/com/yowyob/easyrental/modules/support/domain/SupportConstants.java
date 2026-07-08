package com.yowyob.easyrental.modules.support.domain;

/**
 * Helpers for anonymous landing support sessions.
 */
public final class SupportConstants {

    public static final String ANONYMOUS_EMAIL_DOMAIN = "@anonymous.easyrental.local";
    public static final String ANONYMOUS_EMAIL_PREFIX = "session:";

    private SupportConstants() {
    }

    public static String anonymousEmail(String sessionId) {
        return ANONYMOUS_EMAIL_PREFIX + sessionId.trim() + ANONYMOUS_EMAIL_DOMAIN;
    }

    public static boolean isAnonymousEmail(String email) {
        return email != null && email.contains(ANONYMOUS_EMAIL_PREFIX) && email.endsWith(ANONYMOUS_EMAIL_DOMAIN);
    }
}
