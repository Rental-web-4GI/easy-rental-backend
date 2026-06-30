package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.domain.port.out.KernelHttpPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Administration (roles, permissions) against kernel-core.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
public class KernelAdministrationAdapter {

    private final KernelHttpPort kernelHttpPort;

    public Flux<JsonNode> listPermissions(KernelRequestContext context) {
        return kernelHttpPort.get("/api/administration/permissions", context)
                .flatMapMany(node -> node.isArray() ? Flux.fromIterable(node) : Flux.just(node));
    }

    public Flux<JsonNode> listRoles(KernelRequestContext context) {
        return kernelHttpPort.get("/api/administration/roles", context)
                .flatMapMany(this::flattenRoles);
    }

    public Mono<JsonNode> createDefaultRoles(KernelRequestContext context) {
        return kernelHttpPort.post("/api/administration/roles/defaults", Map.of(), context);
    }

    private Flux<JsonNode> flattenRoles(JsonNode node) {
        if (node == null || node.isNull()) {
            return Flux.empty();
        }
        if (node.isArray()) {
            return Flux.fromIterable(node);
        }
        if (node.has("roles") && node.get("roles").isArray()) {
            return Flux.fromIterable(node.get("roles"));
        }
        if (node.has("content") && node.get("content").isArray()) {
            return Flux.fromIterable(node.get("content"));
        }
        if (node.has("id")) {
            return Flux.just(node);
        }
        return Flux.empty();
    }

    public Mono<JsonNode> createRole(Map<String, Object> payload, KernelRequestContext context) {
        return kernelHttpPort.post("/api/administration/roles", payload, context);
    }

    public Mono<JsonNode> assignRole(UUID userId, Map<String, Object> payload, KernelRequestContext context) {
        return kernelHttpPort.post("/api/administration/users/" + userId + "/roles", payload, context);
    }

    public Mono<JsonNode> inviteEmployee(
            UUID organizationId, Map<String, Object> payload, KernelRequestContext context) {
        KernelRequestContext orgContext = KernelRequestContext.builder()
                .bearerToken(context.bearerToken())
                .organizationId(java.util.Optional.of(organizationId))
                .agencyId(context.agencyId())
                .build();
        return kernelHttpPort.post(
                "/api/employees/invite?organizationId=" + organizationId,
                payload,
                orgContext);
    }

    public Flux<JsonNode> listEmployees(UUID organizationId, KernelRequestContext context) {
        KernelRequestContext orgContext = KernelRequestContext.builder()
                .bearerToken(context.bearerToken())
                .organizationId(java.util.Optional.of(organizationId))
                .agencyId(context.agencyId())
                .build();
        return kernelHttpPort.get("/api/employees?organizationId=" + organizationId, orgContext)
                .flatMapMany(node -> node.isArray() ? Flux.fromIterable(node) : Flux.just(node));
    }
}
