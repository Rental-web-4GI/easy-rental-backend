package com.yowyob.easyrental.kernel.domain.port.out;

import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Optional hybrid driver provisionning (hrm / actor-core).
 * Local drivers stay in PostgreSQL until this port is wired in prod.
 *
 * @author Easy Rental Team
 * @since 2026-07-08
 */
public interface KernelDriverPort {

    /**
     * Create or link a driver actor in kernel; returns kernel actor id.
     */
    Mono<UUID> provisionDriver(Map<String, Object> payload, KernelRequestContext context);
}
