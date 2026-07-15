package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import com.yowyob.easyrental.kernel.infrastructure.dto.KernelLoginResult;
import com.yowyob.easyrental.kernel.domain.port.out.KernelHttpPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Auth operations against kernel-core.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
public class KernelAuthAdapter {

    private final WebClient kernelWebClient;
    private final KernelClientProperties properties;
    private final KernelHttpPort kernelHttpPort;

    public Mono<KernelLoginResult> login(String principal, String password) {
        return kernelWebClient.post()
                .uri("/api/auth/login")
                .headers(this::applyMachineHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("principal", principal, "password", password))
                .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                        .flatMap(body -> KernelResponseSupport.unwrapData(body)
                                .map(data -> {
                                    if (data.has("nextStep")
                                            && "CONFIRM_MFA".equals(data.path("nextStep").asText())) {
                                        return KernelLoginResult.mfaRequired(
                                                data.path("mfaToken").asText(),
                                                data.path("channel").asText(null));
                                    }
                                    String accessToken = resolveAccessToken(data);
                                    if (accessToken == null) {
                                        throw new com.yowyob.easyrental.shared.exception.ValidationException(
                                                "KERNEL_LOGIN_FAILED: Kernel login did not return an access token. "
                                                        + "Check kernel-core.credentials.env.");
                                    }
                                    return KernelLoginResult.authenticated(accessToken);
                                })))
                .onErrorMap(this::mapLoginError);
    }

    private static String resolveAccessToken(JsonNode data) {
        String token = KernelResponseSupport.textOrNull(data, "accessToken");
        if (token == null) {
            token = KernelResponseSupport.textOrNull(data, "access_token");
        }
        return token != null && !token.isBlank() ? token : null;
    }

    private Throwable mapLoginError(Throwable ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        if (message.contains("timed out") || message.contains("Timeout")) {
            return new com.yowyob.easyrental.shared.exception.ValidationException(
                    "KERNEL_TIMEOUT: Connexion au kernel trop lente. Réessayez dans quelques secondes.");
        }
        return ex;
    }

    public Mono<KernelLoginResult> confirmMfa(String mfaToken, String code) {
        return kernelWebClient.post()
                .uri("/api/auth/login/mfa/confirm")
                .headers(this::applyMachineHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("mfaToken", mfaToken, "code", code))
                .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                        .flatMap(KernelResponseSupport::unwrapData)
                        .map(data -> KernelLoginResult.authenticated(data.path("accessToken").asText())));
    }

    public Mono<KernelLoginResult> refresh(String refreshToken) {
        return kernelWebClient.post()
                .uri("/api/auth/refresh")
                .headers(this::applyMachineHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("refreshToken", refreshToken))
                .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                        .flatMap(body -> KernelResponseSupport.unwrapData(body)
                                .map(data -> {
                                    String newToken = resolveAccessToken(data);
                                    if (newToken == null) {
                                        throw new com.yowyob.easyrental.shared.exception.ValidationException(
                                                "KERNEL_REFRESH_FAILED: Le refresh du token kernel a échoué.");
                                    }
                                    return KernelLoginResult.authenticated(newToken);
                                })));
    }

    public Mono<JsonNode> requestEmailVerification(KernelRequestContext context) {
        return kernelWebClient.post()
                .uri("/api/auth/email-verification/request")
                .headers(headers -> {
                    this.applyMachineHeaders(headers);
                    context.bearerToken().ifPresent(token -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token));
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of())
                .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                        .flatMap(KernelResponseSupport::unwrapData));
    }

    public Mono<JsonNode> confirmEmailVerification(String verificationToken, KernelRequestContext context) {
        return kernelWebClient.post()
                .uri("/api/auth/email-verification/confirm")
                .headers(headers -> {
                    this.applyMachineHeaders(headers);
                    context.bearerToken().ifPresent(token -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token));
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("verificationToken", verificationToken))
                .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                        .flatMap(KernelResponseSupport::unwrapData));
    }

    public Mono<JsonNode> getCurrentUser(KernelRequestContext context) {
        return kernelHttpPort.get("/api/users/me", context);
    }

    public Mono<JsonNode> signUp(Map<String, Object> payload) {
        return kernelHttpPort.post("/api/auth/sign-up", payload, KernelRequestContext.empty());
    }

    private void applyMachineHeaders(HttpHeaders headers) {
        if (properties.getClientId() == null || properties.getClientId().isBlank()
                || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new com.yowyob.easyrental.shared.exception.ValidationException(
                    "KERNEL_NOT_CONFIGURED: kernel.client-id / kernel.api-key missing. "
                            + "Copy kernel-core.credentials.env and restart the backend.");
        }
        headers.set("X-Client-Id", properties.getClientId());
        headers.set("X-Api-Key", properties.getApiKey());
        headers.set("X-Tenant-Id", properties.getTenantId());
    }
}
