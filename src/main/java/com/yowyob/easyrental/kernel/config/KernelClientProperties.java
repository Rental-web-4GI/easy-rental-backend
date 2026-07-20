package com.yowyob.easyrental.kernel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kernel-core HTTP client configuration.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "kernel")
public class KernelClientProperties {

    private boolean integrationEnabled = false;
    private String baseUrl = "https://kernel-core.yowyob.com/kernel-api";
    private String clientId = "";
    private String apiKey = "";
    private String tenantId = "11111111-1111-1111-1111-111111111111";
    private String jwksUri = "";
    private String adminUsername = "";
    private String adminPassword = "";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
    private int fileUploadTimeoutMs = 300000;
    private int fileUploadMaxBytes = 16 * 1024 * 1024;

    public String resolvedJwksUri() {
        if (jwksUri != null && !jwksUri.isBlank()) {
            return jwksUri;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/.well-known/jwks.json";
    }
}
