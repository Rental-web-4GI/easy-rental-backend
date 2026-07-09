package com.yowyob.easyrental.kernel.domain.port.out;

import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Optional outbound audit sync toward kernel administration-core.
 * Local audits table remains source of truth until sync is proven.
 *
 * @author Easy Rental Team
 * @since 2026-07-08
 */
public interface KernelAuditPort {

    Mono<Void> publish(Map<String, Object> auditPayload);
}
