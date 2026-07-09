package com.yowyob.easyrental.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Easy Rental application-level settings (agency URLs, mail, kernel hybrid flags).
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
    private Org org = new Org();
    private Vehicle vehicle = new Vehicle();
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
        /** Local-only: create staff in PostgreSQL without kernel sign-up / invite. */
        private boolean skipKernelProvisioning = false;
        /**
         * Never block staff onboarding on kernel email verification.
         * When true, EMAIL_VERIFICATION_REQUIRED falls back to local credentials / deferred invite.
         */
        private boolean skipEmailVerification = true;
    }

    @Getter
    @Setter
    public static class Client {
        /**
         * Local: register and login clients via PostgreSQL only (no kernel email verification).
         * Prod: false — client auth goes through kernel.
         */
        private boolean skipKernelAuth = false;
    }

    @Getter
    @Setter
    public static class Org {
        /**
         * When true (prod), organization owner sign-up requires kernel email verification.
         * When false (local), EMAIL_VERIFICATION_REQUIRED does not hard-block local hybrid onboarding.
         */
        private boolean requireEmailVerification = false;
    }

    @Getter
    @Setter
    public static class Vehicle {
        /**
         * When true, resource-core failures (quota, timeout, session) fall back to local vehicle create.
         * Local: true. Prod: false.
         */
        private boolean allowLocalFallback = true;
    }

    @Getter
    @Setter
    public static class Mail {
        private boolean enabled = false;
        private String from = "noreply@easyrental.local";
    }
}
