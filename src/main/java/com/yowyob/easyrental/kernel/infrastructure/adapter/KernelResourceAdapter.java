package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.domain.port.out.KernelHttpPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Material resources (vehicles, equipment) against resource-core.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
public class KernelResourceAdapter {

    private final KernelHttpPort kernelHttpPort;

    public Mono<JsonNode> createResource(
            UUID organizationId,
            UUID agencyId,
            Map<String, Object> payload,
            KernelRequestContext context) {
        KernelRequestContext orgContext = KernelRequestContext.builder()
                .bearerToken(context.bearerToken())
                .organizationId(java.util.Optional.of(organizationId))
                .agencyId(java.util.Optional.ofNullable(agencyId))
                .build();
        payload.putIfAbsent("organizationId", organizationId.toString());
        if (agencyId != null) {
            payload.putIfAbsent("agencyId", agencyId.toString());
        }
        return kernelHttpPort.post("/api/resources", payload, orgContext);
    }

    public Mono<JsonNode> getResource(UUID resourceId, KernelRequestContext context) {
        return kernelHttpPort.get("/api/resources/" + resourceId, context);
    }
}
