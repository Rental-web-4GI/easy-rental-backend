package com.yowyob.easyrental.kernel.infrastructure;

import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;
import java.util.UUID;

/**
 * Reactor context accessor for kernel request propagation.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public final class KernelContextHolder {

    public static final String CONTEXT_KEY = "kernelRequestContext";

    private KernelContextHolder() {
    }

    public static Context withContext(Context context, KernelRequestContext kernelContext) {
        return context.put(CONTEXT_KEY, kernelContext);
    }

    public static Mono<KernelRequestContext> current() {
        return Mono.deferContextual(ctx -> Mono.just(
                ctx.getOrDefault(CONTEXT_KEY, KernelRequestContext.empty())));
    }

    public static KernelRequestContext fromBearer(Optional<String> bearer) {
        return KernelRequestContext.builder().bearerToken(bearer).build();
    }

    public static KernelRequestContext fromBearerAndOrg(Optional<String> bearer, Optional<UUID> orgId) {
        return KernelRequestContext.builder()
                .bearerToken(bearer)
                .organizationId(orgId)
                .build();
    }
}
