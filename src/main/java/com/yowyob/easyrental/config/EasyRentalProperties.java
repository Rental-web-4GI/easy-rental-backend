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
    private Client client = new Client();
    private Support support = new Support();
    private Admin admin = new Admin();

    @Getter
    @Setter
    public static class Admin {
        /** Upsert platform ADMIN user on startup (local dev). */
        private boolean bootstrapEnabled = false;
        /** Defaults to support admin email when blank. */
        private String email;
        private String password = "password123";
        private String firstname = "Platform";
        private String lastname = "Admin";
    }

    @Getter
    @Setter
    public static class Support {
        private String adminEmail = "rentalreseau01@gmail.com";
        private String consoleUrl = "http://localhost:3004/admin";
        private String helpUrl = "http://localhost:3000/help";
    }

    @Getter
    @Setter
    public static class Staff {
        /** Local dev: create staff in PostgreSQL only, skip kernel sign-up / invite / email verification. */
        private boolean skipKernelProvisioning = false;
    }

    @Getter
    @Setter
    public static class Client {
        /**
         * Local dev: register and login clients via PostgreSQL only (no kernel email verification).
         */
        private boolean skipKernelAuth = false;
    }

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = false;
        private String from = "noreply@easyrental.local";
    }
}
