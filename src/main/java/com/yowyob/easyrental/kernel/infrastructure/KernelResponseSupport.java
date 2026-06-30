package com.yowyob.easyrental.kernel.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.shared.exception.ValidationException;
import reactor.core.publisher.Mono;

/**
 * Helpers for kernel API envelope responses.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public final class KernelResponseSupport {

    private KernelResponseSupport() {
    }

    public static Mono<JsonNode> unwrapData(JsonNode root) {
        if (root == null || root.isNull()) {
            return Mono.error(new ValidationException("Empty kernel response"));
        }
        if (root.has("success") && !root.get("success").asBoolean(true)) {
            String code = root.path("errorCode").asText("KERNEL_ERROR");
            String message = root.path("message").asText("Kernel request failed");
            return Mono.error(new ValidationException(code + ": " + message));
        }
        if (root.has("data") && !root.get("data").isNull()) {
            return Mono.just(root.get("data"));
        }
        return Mono.just(root);
    }

    public static String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
