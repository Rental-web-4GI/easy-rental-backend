package com.yowyob.easyrental.kernel.security;

import com.yowyob.easyrental.kernel.domain.KernelAuthClaims;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KernelPermissionMapperTest {

    @Test
    void shouldExtractOrganizationIdFromScopedPermissions() {
        UUID orgId = UUID.fromString("a15846ce-85a7-44f9-93c3-4acafeddb5b7");
        KernelAuthClaims claims = new KernelAuthClaims(
                UUID.randomUUID().toString(),
                "owner@test.com",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(UUID.randomUUID()),
                List.of("hrm:onboarding:manage#ORGANIZATION:" + orgId),
                List.of("ORGANIZATION_ADMIN"));

        Optional<UUID> resolved = KernelPermissionMapper.organizationIdFromPermissions(claims);

        assertTrue(resolved.isPresent());
        assertEquals(orgId, resolved.get());
    }

    @Test
    void shouldMapScopedVehicleWriteToCreateTag() {
        UUID orgId = UUID.fromString("232c3439-1f4c-40ca-9548-342fd75a981a");
        KernelAuthClaims claims = new KernelAuthClaims(
                UUID.randomUUID().toString(),
                "staff@test.com",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(UUID.randomUUID()),
                List.of("rental:vehicle:write#ORGANIZATION:" + orgId),
                List.of("AGENCY_STAFF"));

        assertTrue(KernelPermissionMapper.hasKernelPermission(claims, "vehicle:create"));
        assertEquals(List.of("vehicle:create"), KernelPermissionMapper.toFrontendTags(claims));
    }
}
