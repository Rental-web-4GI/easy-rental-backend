package com.yowyob.easyrental.kernel.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Parsed claims from a kernel RS256 JWT.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public record KernelAuthClaims(
        String subject,
        String principal,
        Optional<UUID> tenantId,
        Optional<UUID> organizationId,
        Optional<UUID> agencyId,
        Optional<UUID> actorId,
        List<String> permissions,
        List<String> roles
) {
}
