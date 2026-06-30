package com.yowyob.easyrental.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Easy Rental application-level settings (agency URLs, mail).
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "easy-rental")
public class EasyRentalProperties {

    private String agencyLoginUrl = "http://localhost:3002/agency/login";
    private Mail mail = new Mail();
    private Staff staff = new Staff();

    @Getter
    @Setter
    public static class Staff {
        /** Local dev: create staff in PostgreSQL only, skip kernel sign-up / invite / email verification. */
        private boolean skipKernelProvisioning = false;
    }

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = false;
        private String from = "noreply@easyrental.local";
    }
}
