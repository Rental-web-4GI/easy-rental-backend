package com.yowyob.easyrental.kernel.security;

import com.yowyob.easyrental.kernel.domain.KernelAuthClaims;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps kernel permissions to Easy Rental permission tags.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public final class KernelPermissionMapper {

    private static final Map<String, String> KERNEL_TO_TAG = Map.ofEntries(
            Map.entry("rental:vehicle:write", "vehicle:create"),
            Map.entry("rental:vehicle:read", "vehicle:list"),
            Map.entry("rental:vehicle:delete", "vehicle:delete"),
            Map.entry("rental:booking:write", "rental:create"),
            Map.entry("rental:driver:write", "driver:create"),
            Map.entry("rental:driver:read", "driver:list"),
            Map.entry("rental:driver:delete", "driver:delete"),
            Map.entry("administration:assignments:write", "staff:create"),
            Map.entry("organizations:write", "agency:update")
    );

    private static final Map<String, String> TAG_TO_KERNEL = invert(KERNEL_TO_TAG);

    private static final Set<String> OWNER_PERMISSIONS = Set.of(
            "tenant:admin",
            "organizations:write",
            "iam:admin",
            "system:admin");

    private static final Pattern ORGANIZATION_PERMISSION_REF = Pattern.compile(
            "#ORGANIZATION:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    private KernelPermissionMapper() {
    }

    public static boolean isOrganizationOwner(KernelAuthClaims claims) {
        return claims.permissions().stream().anyMatch(OWNER_PERMISSIONS::contains)
                || claims.roles().stream().anyMatch(r -> r.equalsIgnoreCase("OWNER"));
    }

    /**
     * Business actor without org yet (onboarding) — org console access before kernel org is created.
     */
    public static boolean isOrganizationOwnerCandidate(KernelAuthClaims claims) {
        return claims.actorId().isPresent()
                && claims.organizationId().isEmpty()
                && !isAgencyStaff(claims);
    }

    public static boolean isAgencyStaff(KernelAuthClaims claims) {
        return claims.permissions().stream().anyMatch(p -> p.startsWith("rental:"))
                || claims.roles().stream().anyMatch(r -> r.contains("STAFF") || r.contains("AGENT"));
    }

    /**
     * Kernel may omit {@code oid} while still scoping permissions to an organization.
     */
    public static Optional<UUID> organizationIdFromPermissions(KernelAuthClaims claims) {
        for (String permission : claims.permissions()) {
            Matcher matcher = ORGANIZATION_PERMISSION_REF.matcher(permission);
            if (matcher.find()) {
                return Optional.of(UUID.fromString(matcher.group(1)));
            }
        }
        return Optional.empty();
    }

    public static boolean hasKernelPermission(KernelAuthClaims claims, String frontendTag) {
        if (isOrganizationOwner(claims)) {
            return true;
        }
        if ("vehicle:update".equals(frontendTag) || "vehiclecategory:update".equals(frontendTag)) {
            return permissionsContain(claims.permissions(), "rental:vehicle:write", "vehicle:update");
        }
        if ("driver:update".equals(frontendTag)) {
            return permissionsContain(claims.permissions(), "rental:driver:write", "driver:update");
        }
        String kernelPermission = TAG_TO_KERNEL.get(frontendTag);
        if (kernelPermission != null && permissionsContain(claims.permissions(), kernelPermission, frontendTag)) {
            return true;
        }
        return permissionsContain(claims.permissions(), null, frontendTag);
    }

    public static List<String> toFrontendTags(KernelAuthClaims claims) {
        if (isOrganizationOwner(claims)) {
            return KERNEL_TO_TAG.values().stream().distinct().toList();
        }
        return claims.permissions().stream()
                .map(p -> KERNEL_TO_TAG.getOrDefault(permissionBase(p), permissionBase(p)))
                .distinct()
                .toList();
    }

    private static String permissionBase(String permission) {
        int scopeIdx = permission.indexOf('#');
        return scopeIdx >= 0 ? permission.substring(0, scopeIdx) : permission;
    }

    private static boolean permissionsContain(
            List<String> permissions, String kernelPermission, String frontendTag) {
        for (String raw : permissions) {
            String base = permissionBase(raw);
            if (kernelPermission != null && base.equals(kernelPermission)) {
                return true;
            }
            if (frontendTag != null && base.equals(frontendTag)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> invert(Map<String, String> source) {
        Map<String, String> inverted = new HashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            inverted.putIfAbsent(entry.getValue(), entry.getKey());
        }
        return Map.copyOf(inverted);
    }
}
