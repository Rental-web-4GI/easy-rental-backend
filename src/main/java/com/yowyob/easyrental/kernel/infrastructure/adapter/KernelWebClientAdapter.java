package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.domain.port.out.KernelHttpPort;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * WebClient adapter for kernel-core HTTP API.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
public class KernelWebClientAdapter implements KernelHttpPort {

    private final WebClient kernelWebClient;
    private final KernelClientProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<JsonNode> get(String path, KernelRequestContext context) {
        return exchange("GET", path, null, context);
    }

    @Override
    public Mono<JsonNode> post(String path, Object body, KernelRequestContext context) {
        return exchange("POST", path, body, context);
    }

    @Override
    public Mono<JsonNode> put(String path, Object body, KernelRequestContext context) {
        return exchange("PUT", path, body, context);
    }

    @Override
    public Mono<JsonNode> delete(String path, KernelRequestContext context) {
        return exchange("DELETE", path, null, context);
    }

    @Override
    public Mono<JsonNode> post(String path, Map<String, Object> body, KernelRequestContext context) {
        return post(path, (Object) body, context);
    }

    private Mono<JsonNode> exchange(String method, String path, Object body, KernelRequestContext context) {
        WebClient.RequestBodySpec spec = kernelWebClient.method(org.springframework.http.HttpMethod.valueOf(method))
                .uri(path)
                .headers(headers -> applyHeaders(headers, context))
                .accept(MediaType.APPLICATION_JSON);

        Mono<JsonNode> responseMono;
        if (body != null) {
            responseMono = spec.contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class);
        } else {
            responseMono = spec.retrieve().bodyToMono(JsonNode.class);
        }

        return responseMono
                .flatMap(KernelResponseSupport::unwrapData)
                .onErrorMap(WebClientResponseException.class, this::mapHttpError)
                .onErrorMap(ex -> !(ex instanceof ValidationException), this::mapConnectionError);
    }

    private Throwable mapConnectionError(Throwable ex) {
        if (ex instanceof ValidationException validationException) {
            return validationException;
        }
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        if (message.contains("timed out") || message.contains("Timeout")
                || message.contains("Connection refused")
                || message.contains("kernel-core")
                || message.contains(":443")) {
            return new ValidationException(
                    "KERNEL_UNAVAILABLE: Impossible de joindre le serveur kernel. Réessayez dans quelques secondes.");
        }
        return new ValidationException("KERNEL_ERROR: " + message);
    }

    private void applyHeaders(HttpHeaders headers, KernelRequestContext context) {
        headers.set("X-Client-Id", properties.getClientId());
        headers.set("X-Api-Key", properties.getApiKey());
        headers.set("X-Tenant-Id", properties.getTenantId());
        context.bearerToken().ifPresent(token -> headers.setBearerAuth(token));
        context.organizationId()
                .map(UUID::toString)
                .ifPresent(orgId -> headers.set("X-Organization-Id", orgId));
        context.agencyId()
                .map(UUID::toString)
                .ifPresent(agencyId -> headers.set("X-Agency-Id", agencyId));
    }

    private Throwable mapHttpError(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        try {
            JsonNode node = objectMapper.readTree(body);
            String code = node.path("errorCode").asText("HTTP_" + ex.getStatusCode().value());
            String message = node.hasNonNull("message")
                    ? node.get("message").asText()
                    : node.path("error").asText(ex.getStatusText());
            return new ValidationException(code + ": " + message);
        } catch (Exception parseError) {
            return new ValidationException("HTTP_" + ex.getStatusCode().value() + ": " + ex.getStatusText());
        }
    }
}
