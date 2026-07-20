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

    /** Canonical constructor : normalise null → Optional.empty() sur tous les champs. */
    public KernelRequestContext {
        bearerToken = bearerToken != null ? bearerToken : Optional.empty();
        organizationId = organizationId != null ? organizationId : Optional.empty();
        agencyId = agencyId != null ? agencyId : Optional.empty();
    }

    public static KernelRequestContext empty() {
        return new KernelRequestContext(Optional.empty(), Optional.empty(), Optional.empty());
    }

    /** Raccourci pour ne set que le bearer, org/agency restent Optional.empty(). */
    public static KernelRequestContext ofBearer(String token) {
        return new KernelRequestContext(Optional.ofNullable(token), Optional.empty(), Optional.empty());
    }
}
