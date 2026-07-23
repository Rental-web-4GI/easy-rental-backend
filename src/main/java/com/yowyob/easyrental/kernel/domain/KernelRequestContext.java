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
        Optional<UUID> agencyId,
        boolean anonymous
) {

    /** Canonical constructor : normalise null → Optional.empty() sur tous les champs. */
    public KernelRequestContext {
        bearerToken = bearerToken != null ? bearerToken : Optional.empty();
        organizationId = organizationId != null ? organizationId : Optional.empty();
        agencyId = agencyId != null ? agencyId : Optional.empty();
    }

    public static KernelRequestContext empty() {
        return new KernelRequestContext(Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    /**
     * Contexte explicitement anonyme : aucun bearer, aucun fallback sur le
     * token app platform-admin. À utiliser pour les endpoints publics
     * d'auth (sign-up, login, discover-contexts) que Kernel refuse quand
     * ils sont appelés avec un Authorization non vide.
     */
    public static KernelRequestContext publicEndpoint() {
        return new KernelRequestContext(Optional.empty(), Optional.empty(), Optional.empty(), true);
    }

    /** Raccourci pour ne set que le bearer, org/agency restent Optional.empty(). */
    public static KernelRequestContext ofBearer(String token) {
        return new KernelRequestContext(Optional.ofNullable(token), Optional.empty(), Optional.empty(), false);
    }
}
