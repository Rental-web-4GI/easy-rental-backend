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
 * Adapter for kernel tp-core (third-party / client registry) operations.
 *
 * @author Easy Rental Team
 * @since 2026-07-15
 */
@Component
@RequiredArgsConstructor
public class KernelTpAdapter {

    private final KernelHttpPort kernelHttpPort;

    public Mono<JsonNode> createClient(Map<String, Object> payload, KernelRequestContext context) {
        return kernelHttpPort.post("/api/clients", payload, context);
    }

    public Mono<JsonNode> getClient(UUID kernelClientId, KernelRequestContext context) {
        return kernelHttpPort.get("/api/clients/" + kernelClientId, context);
    }
}
