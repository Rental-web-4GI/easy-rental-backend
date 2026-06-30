package com.yowyob.easyrental.kernel.application;

import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Subscribes newly created organisations to required kernel services.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KernelOrganizationBootstrapService {

    private static final List<String> DEFAULT_SERVICES = List.of(
            "ORGANIZATION", "HRM", "SETTINGS", "RESOURCE", "COMMERCIAL");

    private final KernelOrganizationAdapter kernelOrganizationAdapter;

    public Mono<Void> subscribeDefaultServices(UUID kernelOrganizationId, KernelRequestContext context) {
        return Flux.fromIterable(DEFAULT_SERVICES)
                .concatMap(serviceCode -> ensureService(kernelOrganizationId, serviceCode, context))
                .then();
    }

    /**
     * Idempotent subscription to a kernel service for an organisation.
     */
    public Mono<Void> ensureService(UUID kernelOrganizationId, String serviceCode, KernelRequestContext context) {
        return kernelOrganizationAdapter.subscribeService(kernelOrganizationId, serviceCode, context)
                .then()
                .onErrorResume(ex -> {
                    if (isAlreadySubscribed(ex)) {
                        return Mono.empty();
                    }
                    log.warn("Kernel service subscription skipped for {} on org {}: {}",
                            serviceCode, kernelOrganizationId, ex.getMessage());
                    return Mono.empty();
                });
    }

    private boolean isAlreadySubscribed(Throwable ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        return message.contains("ALREADY")
                || message.contains("SUBSCRIBED")
                || message.contains("DUPLICATE");
    }
}
