package com.yowyob.easyrental.kernel.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Obtains and periodically refreshes an app-level kernel access token for
 * machine-to-machine calls (e.g. file uploads) that cannot rely on a per-user
 * session token.
 *
 * <p>Uses the kernel discover-contexts + select-context flow with admin
 * credentials — no MFA required. Runs on startup and every 10 min.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KernelAppTokenProvider {

    private final WebClient kernelWebClient;
    private final KernelClientProperties kernelProperties;

    private volatile String cachedToken;
    private volatile long expiresAtEpochSeconds;

    /** Non-blocking accessor. Returns the cached token if any (may be stale by up to ~10 min). */
    public Optional<String> currentToken() {
        long now = System.currentTimeMillis() / 1000L;
        if (cachedToken != null && expiresAtEpochSeconds > now) {
            long remaining = expiresAtEpochSeconds - now;
            log.debug("[kernel-app-token] currentToken() cache hit ({}s remaining)", remaining);
            return Optional.of(cachedToken);
        }
        log.debug("[kernel-app-token] currentToken() cache empty or expired");
        return Optional.empty();
    }

    /**
     * Force refresh + return the freshest possible token.
     * À utiliser pour les opérations admin critiques où on veut être sûr que
     * le token est bien accepté par le kernel (le cache pourrait avoir subi
     * une invalidation server-side qu'on ne détecte pas).
     */
    public Mono<Optional<String>> freshToken() {
        String username = kernelProperties.getAdminUsername();
        String password = kernelProperties.getAdminPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Mono.just(Optional.empty());
        }
        return discoverAndSelect(username, password)
                .map(token -> {
                    cachedToken = token;
                    expiresAtEpochSeconds = extractExpiry(token);
                    long ttl = expiresAtEpochSeconds - (System.currentTimeMillis() / 1000L);
                    log.info("[kernel-app-token] freshToken() force-refresh — valid {}s ({}min)", ttl, ttl / 60);
                    return Optional.of(token);
                })
                .onErrorResume(ex -> {
                    log.warn("[kernel-app-token] freshToken() failed: {}", ex.getMessage());
                    return Mono.just(Optional.ofNullable(cachedToken));
                });
    }

    @PostConstruct
    void bootstrap() {
        if (!kernelProperties.isIntegrationEnabled()) {
            return;
        }
        refreshAsync("startup");
    }

    /** Refresh every 10 min (access tokens live 15 min). */
    @Scheduled(fixedDelay = 600_000L, initialDelay = 600_000L)
    void scheduledRefresh() {
        if (kernelProperties.isIntegrationEnabled()) {
            refreshAsync("scheduled");
        }
    }

    private void refreshAsync(String reason) {
        String username = kernelProperties.getAdminUsername();
        String password = kernelProperties.getAdminPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("[kernel-app-token] admin credentials not configured — cannot obtain app token");
            return;
        }
        discoverAndSelect(username, password)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        token -> {
                            cachedToken = token;
                            expiresAtEpochSeconds = extractExpiry(token);
                            long ttl = expiresAtEpochSeconds - (System.currentTimeMillis() / 1000L);
                            log.info("[kernel-app-token] {} refresh — token valid {}s ({}min)",
                                    reason, ttl, ttl / 60);
                        },
                        ex -> log.warn("[kernel-app-token] {} refresh failed: {}", reason, ex.getMessage()));
    }

    private Mono<String> discoverAndSelect(String username, String password) {
        return kernelWebClient.post()
                .uri("/api/auth/discover-contexts")
                .headers(this::applyMachineHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("principal", username, "password", password))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(this::extractContextAndSelect);
    }

    private Mono<String> extractContextAndSelect(JsonNode discoverResponse) {
        JsonNode data = unwrap(discoverResponse);
        if (data == null) {
            return Mono.error(new RuntimeException("discover-contexts returned no data"));
        }
        String selectionToken = data.path("selectionToken").asText(null);
        JsonNode contexts = data.path("contexts");
        if (selectionToken == null || !contexts.isArray() || contexts.isEmpty()) {
            return Mono.error(new RuntimeException("discover-contexts missing selectionToken or contexts"));
        }
        JsonNode firstCtx = contexts.get(0);
        String contextId = firstCtx.path("contextId").asText(null);
        if (contextId == null) {
            return Mono.error(new RuntimeException("no contextId in discover response"));
        }

        // IMPORTANT: NE PAS envoyer organizationId — on veut un token TENANT-WIDE
        // (avec tenant:admin) pour les opérations admin cross-organisation (approve, reject, etc.).
        // Si on set organizationId, le kernel émet un token scopé à cette org (avec "oid" dans le JWT),
        // ce qui provoque WWW-Authenticate: Basic sur les endpoints hors-scope.
        Map<String, Object> body = new HashMap<>();
        body.put("selectionToken", selectionToken);
        body.put("contextId", contextId);
        return kernelWebClient.post()
                .uri("/api/auth/select-context")
                .headers(this::applyMachineHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(this::extractAccessToken);
    }

    private Mono<String> extractAccessToken(JsonNode selectResponse) {
        JsonNode data = unwrap(selectResponse);
        JsonNode session = data != null ? data.path("session") : null;
        String accessToken = session != null ? session.path("accessToken").asText(null) : null;
        if (accessToken == null) {
            return Mono.error(new RuntimeException("select-context missing accessToken"));
        }
        return Mono.just(accessToken);
    }

    private JsonNode unwrap(JsonNode response) {
        if (response == null) {
            return null;
        }
        if (response.has("success") && !response.get("success").asBoolean(true)) {
            log.warn("[kernel-app-token] kernel error: {}", response.path("message").asText());
            return null;
        }
        return response.has("data") ? response.get("data") : response;
    }

    private void applyMachineHeaders(HttpHeaders headers) {
        headers.set("X-Client-Id", kernelProperties.getClientId());
        headers.set("X-Api-Key", kernelProperties.getApiKey());
        headers.set("X-Tenant-Id", kernelProperties.getTenantId());
    }

    private long extractExpiry(String jwt) {
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            return 0L;
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            String json = new String(payload);
            int idx = json.indexOf("\"exp\"");
            if (idx < 0) {
                return 0L;
            }
            int colon = json.indexOf(':', idx);
            int end = colon + 1;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == ' ')) {
                end++;
            }
            return Long.parseLong(json.substring(colon + 1, end).trim());
        } catch (Exception ex) {
            return 0L;
        }
    }
}
