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
 * Organization and agency operations against kernel-core.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
public class KernelOrganizationAdapter {

    private final KernelHttpPort kernelHttpPort;

    public Mono<JsonNode> createOrganization(Map<String, Object> payload, KernelRequestContext context) {
        return kernelHttpPort.post("/api/organizations", payload, context);
    }

    public Mono<JsonNode> approveOrganization(UUID organizationId, String reason, KernelRequestContext context) {
        KernelRequestContext orgContext = KernelRequestContext.builder()
                .bearerToken(context.bearerToken())
                .organizationId(java.util.Optional.of(organizationId))
                .agencyId(context.agencyId())
                .build();
        return kernelHttpPort.post(
                "/api/organizations/" + organizationId + "/approve",
                Map.of("reason", reason),
                orgContext);
    }

    public Mono<JsonNode> rejectOrganization(UUID organizationId, String reason, KernelRequestContext context) {
        KernelRequestContext orgContext = KernelRequestContext.builder()
                .bearerToken(context.bearerToken())
                .organizationId(java.util.Optional.of(organizationId))
                .agencyId(context.agencyId())
                .build();
        return kernelHttpPort.post(
                "/api/organizations/" + organizationId + "/reject",
                Map.of("reason", reason),
                orgContext);
    }

    public Mono<JsonNode> subscribeService(UUID organizationId, String serviceCode, KernelRequestContext context) {
        KernelRequestContext orgContext = KernelRequestContext.builder()
                .bearerToken(context.bearerToken())
                .organizationId(java.util.Optional.of(organizationId))
                .agencyId(context.agencyId())
                .build();
        return kernelHttpPort.post(
                "/api/organizations/" + organizationId + "/services",
                Map.of(
                        "serviceCode", serviceCode,
                        "requestQuotaLimit", 10000,
                        "requestQuotaWindowSeconds", 3600),
                orgContext);
    }

    public Flux<JsonNode> myOrganizations(KernelRequestContext context) {
        return kernelHttpPort.get("/api/organizations/my", context)
                .flatMapMany(this::flattenOrganizations);
    }

    private Flux<JsonNode> flattenOrganizations(JsonNode node) {
        if (node == null || node.isNull()) {
            return Flux.empty();
        }
        if (node.isArray()) {
            return Flux.fromIterable(node);
        }
        if (node.has("organizations") && node.get("organizations").isArray()) {
            return Flux.fromIterable(node.get("organizations"));
        }
        if (node.has("id")) {
            return Flux.just(node);
        }
        return Flux.empty();
    }

    public Mono<JsonNode> getOrganization(UUID organizationId, KernelRequestContext context) {
        KernelRequestContext orgContext = KernelRequestContext.builder()
                .bearerToken(context.bearerToken())
                .organizationId(java.util.Optional.of(organizationId))
                .agencyId(context.agencyId())
                .build();
        return kernelHttpPort.get("/api/organizations/" + organizationId, orgContext);
    }

    public Mono<JsonNode> createAgency(UUID organizationId, Map<String, Object> payload, KernelRequestContext context) {
        KernelRequestContext orgContext = KernelRequestContext.builder()
                .bearerToken(context.bearerToken())
                .organizationId(java.util.Optional.of(organizationId))
                .agencyId(context.agencyId())
                .build();
        return kernelHttpPort.post("/api/organizations/" + organizationId + "/agencies", payload, orgContext);
    }

    public Flux<JsonNode> listAgencies(UUID organizationId, KernelRequestContext context) {
        KernelRequestContext orgContext = KernelRequestContext.builder()
                .bearerToken(context.bearerToken())
                .organizationId(java.util.Optional.of(organizationId))
                .agencyId(context.agencyId())
                .build();
        return kernelHttpPort.get("/api/organizations/" + organizationId + "/agencies", orgContext)
                .flatMapMany(node -> {
                    if (node.isArray()) {
                        return Flux.fromIterable(node);
                    }
                    return Flux.just(node);
                });
    }

    public Mono<JsonNode> createBusinessActor(Map<String, Object> payload, KernelRequestContext context) {
        return kernelHttpPort.post("/api/actors/onboarding", payload, context);
    }

    public Mono<JsonNode> getMyBusinessActor(KernelRequestContext context) {
        return kernelHttpPort.get("/api/actors/me", context);
    }

    /**
     * Approve/reject un business actor via governance (admin plateforme).
     * @param businessActorProfileId l'id du profil business actor (pas l'actorId user)
     * @param action "APPROVE" ou "REJECT"
     */
    public Mono<JsonNode> governBusinessActor(
            UUID businessActorProfileId, String action, String reason, KernelRequestContext context) {
        return kernelHttpPort.post(
                "/api/administration/governance/business-actors/" + businessActorProfileId,
                Map.of("action", action, "reason", reason),
                context);
    }
}
