package com.yowyob.easyrental.kernel.domain;

import lombok.Builder;

import java.util.Optional;
import java.util.UUID;

/**
 * Per-request context propagated to kernel-core.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Builder
public record KernelRequestContext(
        Optional<String> bearerToken,
        Optional<UUID> organizationId,
        Optional<UUID> agencyId
) {

    public static KernelRequestContext empty() {
        return new KernelRequestContext(Optional.empty(), Optional.empty(), Optional.empty());
    }
}
