package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.domain.port.out.KernelDriverPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Stub driver hybrid adapter. Enable with {@code easy-rental.driver.kernel-hybrid.enabled=true}.
 *
 * @author Easy Rental Team
 * @since 2026-07-08
 */
@Component
@ConditionalOnProperty(prefix = "easy-rental.driver.kernel-hybrid", name = "enabled", havingValue = "true")
public class KernelDriverAdapter implements KernelDriverPort {

    @Override
    public Mono<UUID> provisionDriver(Map<String, Object> payload, KernelRequestContext context) {
        return Mono.error(new UnsupportedOperationException(
                "Driver kernel hybrid is not implemented yet. Keep easy-rental.driver.kernel-hybrid.enabled=false."));
    }
}
