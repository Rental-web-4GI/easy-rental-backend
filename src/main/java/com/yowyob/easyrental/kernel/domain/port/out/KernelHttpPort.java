package com.yowyob.easyrental.kernel.domain.port.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Outbound port for HTTP calls to kernel-core.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public interface KernelHttpPort {

    Mono<JsonNode> get(String path, KernelRequestContext context);

    Mono<JsonNode> post(String path, Object body, KernelRequestContext context);

    Mono<JsonNode> put(String path, Object body, KernelRequestContext context);

    Mono<JsonNode> delete(String path, KernelRequestContext context);

    Mono<JsonNode> post(String path, Map<String, Object> body, KernelRequestContext context);
}
