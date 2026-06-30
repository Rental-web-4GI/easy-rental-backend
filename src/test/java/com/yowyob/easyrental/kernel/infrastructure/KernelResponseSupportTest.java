package com.yowyob.easyrental.kernel.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class KernelResponseSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldUnwrapDataNodeFromEnvelope() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("success", true);
        root.set("data", objectMapper.createObjectNode().put("accessToken", "jwt"));

        StepVerifier.create(KernelResponseSupport.unwrapData(root))
                .expectNextMatches(node -> "jwt".equals(node.get("accessToken").asText()))
                .verifyComplete();
    }

    @Test
    void shouldFailWhenKernelReturnsErrorEnvelope() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("success", false);
        root.put("errorCode", "AUTH_FAILED");
        root.put("message", "Invalid credentials");

        StepVerifier.create(KernelResponseSupport.unwrapData(root))
                .expectErrorMatches(e -> e.getMessage().contains("AUTH_FAILED"))
                .verify();
    }
}
