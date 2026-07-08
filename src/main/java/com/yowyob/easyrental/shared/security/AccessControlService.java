package com.yowyob.easyrental.shared.security;

import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.security.KernelAuthenticationToken;
import com.yowyob.easyrental.kernel.security.KernelPermissionMapper;
import org.springframework.security.core.GrantedAuthority;
import com.yowyob.easyrental.modules.agency.infrastructure.adapter.out.persistence.AgencyRepository;
import com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence.UserRepository;
import com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence.OrganizationRepository;
import com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.persistence.StaffRepository;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.CategoryRepository;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * RBAC service with kernel JWT permissions or local fallback.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Service("rbac")
@RequiredArgsConstructor
public class AccessControlService {

    private final KernelClientProperties kernelProperties;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final VehicleRepository vehicleRepository;
    private final AgencyRepository agencyRepository;
    private final OrganizationRepository organizationRepository;
    private final CategoryRepository categoryRepository;

    private static final Duration RBAC_BLOCK_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Reactive SpEL entry for {@code @PreAuthorize} on WebFlux controllers (returns {@link Mono}).
     */
    public Mono<Void> assertVehicleCreate(UUID orgId, UUID agencyId, Authentication authentication) {
        if (authentication == null) {
            return Mono.error(new AccessDeniedException("Access Denied"));
        }
        if (hasAuthority(authentication, "ROLE_ORGANIZATION") || hasAuthority(authentication, "ROLE_ADMIN")) {
            return Mono.empty();
        }
        return resolvePermission(orgId, "vehicle:create", authentication)
                .flatMap(allowed -> {
                    if (Boolean.TRUE.equals(allowed)) {
                        return Mono.empty();
                    }
                    if (agencyId == null) {
                        return Mono.error(new AccessDeniedException("Access Denied"));
                    }
                    return agencyRepository.findOrgIdByAgencyId(agencyId)
                            .flatMap(agencyOrgId -> localHasPermissionForUser(
                                    agencyOrgId, "vehicle:create", authentication))
                            .filter(Boolean::booleanValue)
                            .switchIfEmpty(Mono.error(new AccessDeniedException("Access Denied")))
                            .then();
                });
    }

    public Mono<Boolean> checkPermissionReactive(Object orgIdObj, String permissionTag) {
        return currentAuthentication()
                .flatMap(auth -> resolvePermission(orgIdObj, permissionTag, auth))
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> checkAgencyAccessReactive(Object agencyIdObj, String tag) {
        return currentAuthentication()
                .flatMap(auth -> {
                    if (kernelProperties.isIntegrationEnabled()
                            && resolveKernelPermission(auth, tag)) {
                        return Mono.just(true);
                    }
                    UUID agencyId = parseUuid(agencyIdObj);
                    return agencyRepository.findOrgIdByAgencyId(agencyId)
                            .flatMap(orgId -> localHasPermissionForUser(orgId, tag, auth))
                            .defaultIfEmpty(false);
                })
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> checkVehicleAccessReactive(Object vehicleIdObj, String tag) {
        String resolvedTag = tag.contains("list") ? "vehicle:list" : "vehicle:update";
        return currentAuthentication()
                .flatMap(auth -> {
                    if (kernelProperties.isIntegrationEnabled()
                            && resolveKernelPermission(auth, resolvedTag)) {
                        return Mono.just(true);
                    }
                    UUID vehicleId = parseUuid(vehicleIdObj);
                    return vehicleRepository.findOrgIdByVehicleId(vehicleId)
                            .flatMap(orgId -> localHasPermissionForUser(orgId, tag, auth))
                            .defaultIfEmpty(false);
                })
                .defaultIfEmpty(false);
    }

    /**
     * Synchronous SpEL entry point kept for non-reactive callers.
     */
    public boolean checkPermission(Object orgIdObj, String permissionTag, Authentication authentication) {
        try {
            Authentication auth = coalesceAuthentication(authentication);
            if (kernelProperties.isIntegrationEnabled()
                    && resolveKernelPermission(auth, permissionTag)) {
                return true;
            }
            return Boolean.TRUE.equals(
                    localHasPermissionForUser(parseOrgId(orgIdObj), permissionTag, auth)
                            .blockOptional(RBAC_BLOCK_TIMEOUT)
                            .orElse(false));
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean hasPermission(Object orgIdObj, String permissionTag, Authentication authentication) {
        return checkPermission(orgIdObj, permissionTag, authentication);
    }

    public boolean checkAgencyAccess(Object agencyIdObj, String tag, Authentication authentication) {
        try {
            if (kernelProperties.isIntegrationEnabled()
                    && resolveKernelPermission(authentication, tag)) {
                return true;
            }
            UUID agencyId = parseUuid(agencyIdObj);
            return Boolean.TRUE.equals(
                    agencyRepository.findOrgIdByAgencyId(agencyId)
                            .flatMap(orgId -> localHasPermissionForUser(orgId, tag, authentication))
                            .defaultIfEmpty(false)
                            .blockOptional(RBAC_BLOCK_TIMEOUT)
                            .orElse(false));
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean checkVehicleAccess(Object vehicleIdObj, String tag, Authentication authentication) {
        try {
            String resolvedTag = tag.contains("list") ? "vehicle:list" : "vehicle:update";
            if (kernelProperties.isIntegrationEnabled()
                    && resolveKernelPermission(authentication, resolvedTag)) {
                return true;
            }
            UUID vehicleId = parseUuid(vehicleIdObj);
            return Boolean.TRUE.equals(
                    vehicleRepository.findOrgIdByVehicleId(vehicleId)
                            .flatMap(orgId -> localHasPermissionForUser(orgId, tag, authentication))
                            .defaultIfEmpty(false)
                            .blockOptional(RBAC_BLOCK_TIMEOUT)
                            .orElse(false));
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean checkStaffAccess(Object staffIdObj, String tag, Authentication authentication) {
        try {
            if (kernelProperties.isIntegrationEnabled()
                    && resolveKernelPermission(authentication, tag)) {
                return true;
            }
            UUID staffId = parseUuid(staffIdObj);
            return Boolean.TRUE.equals(
                    staffRepository.findOrgIdByStaffId(staffId)
                            .flatMap(orgId -> localHasPermissionForUser(orgId, tag, authentication))
                            .defaultIfEmpty(false)
                            .blockOptional(RBAC_BLOCK_TIMEOUT)
                            .orElse(false));
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean checkCategoryAccess(Object categoryIdObj, String tag, Authentication authentication) {
        try {
            if (kernelProperties.isIntegrationEnabled()
                    && resolveKernelPermission(authentication, tag)) {
                return true;
            }
            UUID categoryId = parseUuid(categoryIdObj);
            return Boolean.TRUE.equals(
                    categoryRepository.findOrgIdByCategoryId(categoryId)
                            .flatMap(orgId -> {
                                if (orgId == null) {
                                    return Mono.just(false);
                                }
                                return localHasPermissionForUser(orgId, tag, authentication);
                            })
                            .defaultIfEmpty(false)
                            .blockOptional(RBAC_BLOCK_TIMEOUT)
                            .orElse(false));
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean resolveKernelPermission(Authentication authentication, String permissionTag) {
        if (authentication instanceof KernelAuthenticationToken kernelAuth) {
            return KernelPermissionMapper.hasKernelPermission(kernelAuth.getClaims(), permissionTag);
        }
        return false;
    }

    private Mono<Boolean> resolvePermission(
            Object orgIdObj, String permissionTag, Authentication authentication) {
        if (kernelProperties.isIntegrationEnabled()
                && resolveKernelPermission(authentication, permissionTag)) {
            return Mono.just(true);
        }
        return localHasPermissionForUser(parseOrgId(orgIdObj), permissionTag, authentication);
    }

    private Mono<Authentication> currentAuthentication() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication());
    }

    private Authentication coalesceAuthentication(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getName() != null) {
            return authentication;
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
                .blockOptional(RBAC_BLOCK_TIMEOUT)
                .orElse(authentication);
    }

    private Mono<Boolean> localHasPermissionForUser(
            UUID orgId, String permissionTag, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return Mono.just(false);
        }
        return userRepository.findByEmail(authentication.getName())
                .flatMap(user -> {
                    if ("ADMIN".equals(user.getRole())) {
                        return Mono.just(true);
                    }
                    if ("ORGANIZATION".equals(user.getRole())) {
                        return organizationRepository.findById(Objects.requireNonNull(orgId))
                                .map(org -> org.getOwnerId().equals(user.getId()))
                                .defaultIfEmpty(false);
                    }
                    return staffRepository.checkStaffPermission(user.getId(), orgId, permissionTag);
                })
                .defaultIfEmpty(false);
    }

    private static UUID parseOrgId(Object orgIdObj) {
        if (orgIdObj == null) {
            return null;
        }
        return (orgIdObj instanceof String) ? UUID.fromString((String) orgIdObj) : (UUID) orgIdObj;
    }

    private static UUID parseUuid(Object idObj) {
        return parseOrgId(idObj);
    }

    public Mono<Boolean> hasPermission(Object orgIdObj, String permissionTag) {
        if (kernelProperties.isIntegrationEnabled()) {
            return kernelHasPermission(permissionTag);
        }
        return localHasPermission(orgIdObj, permissionTag);
    }

    private Mono<Boolean> kernelHasPermission(String permissionTag) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> {
                    if (auth instanceof KernelAuthenticationToken kernelAuth) {
                        return KernelPermissionMapper.hasKernelPermission(
                                kernelAuth.getClaims(), permissionTag);
                    }
                    return false;
                })
                .defaultIfEmpty(false);
    }

    private Mono<Boolean> localHasPermission(Object orgIdObj, String permissionTag) {
        UUID orgId = (orgIdObj instanceof String) ? UUID.fromString((String) orgIdObj) : (UUID) orgIdObj;

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userRepository::findByEmail)
                .flatMap(user -> {
                    if ("ADMIN".equals(user.getRole())) {
                        return Mono.just(true);
                    }
                    if ("ORGANIZATION".equals(user.getRole())) {
                        return organizationRepository.findById(Objects.requireNonNull(orgId))
                                .map(org -> org.getOwnerId().equals(user.getId()))
                                .defaultIfEmpty(false);
                    }
                    return staffRepository.checkStaffPermission(user.getId(), orgId, permissionTag);
                })
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> canAccessVehicle(Object vehicleIdObj, String tag) {
        if (kernelProperties.isIntegrationEnabled()) {
            return hasPermission(null, tag.contains("list") ? "vehicle:list" : "vehicle:update");
        }
        UUID vehicleId = (vehicleIdObj instanceof String) ? UUID.fromString((String) vehicleIdObj)
                : (UUID) vehicleIdObj;
        return vehicleRepository.findOrgIdByVehicleId(vehicleId)
                .flatMap(orgId -> hasPermission(orgId, tag));
    }

    public Mono<Boolean> canAccessAgency(Object agencyIdObj, String tag) {
        if (kernelProperties.isIntegrationEnabled()) {
            return hasPermission(null, tag);
        }
        UUID agencyId = (agencyIdObj instanceof String) ? UUID.fromString((String) agencyIdObj) : (UUID) agencyIdObj;
        return agencyRepository.findOrgIdByAgencyId(agencyId)
                .flatMap(orgId -> hasPermission(orgId, tag));
    }

    public Mono<Boolean> canAccessStaffMember(Object staffIdObj, String tag) {
        if (kernelProperties.isIntegrationEnabled()) {
            return hasPermission(null, tag);
        }
        UUID staffId = (staffIdObj instanceof String) ? UUID.fromString((String) staffIdObj) : (UUID) staffIdObj;
        return staffRepository.findOrgIdByStaffId(staffId)
                .flatMap(orgId -> hasPermission(orgId, tag));
    }

    public Mono<Boolean> canAccessCategory(Object categoryIdObj, String tag) {
        UUID categoryId = (categoryIdObj instanceof String) ? UUID.fromString((String) categoryIdObj)
                : (UUID) categoryIdObj;

        if (kernelProperties.isIntegrationEnabled()) {
            return ReactiveSecurityContextHolder.getContext()
                    .map(ctx -> ctx.getAuthentication())
                    .flatMap(auth -> {
                        if (auth instanceof KernelAuthenticationToken kernelAuth) {
                            return Mono.just(KernelPermissionMapper.hasKernelPermission(
                                    kernelAuth.getClaims(), tag));
                        }
                        if (hasAuthority(auth, "ROLE_ORGANIZATION")) {
                            return categoryRepository.findOrgIdByCategoryId(categoryId)
                                    .flatMap(orgId -> {
                                        if (orgId == null) {
                                            return Mono.just(false);
                                        }
                                        return userRepository.findByEmail(auth.getName())
                                                .flatMap(user -> organizationRepository.findById(orgId)
                                                        .map(org -> org.getOwnerId().equals(user.getId()))
                                                        .defaultIfEmpty(false));
                                    })
                                    .defaultIfEmpty(false);
                        }
                        return Mono.just(false);
                    })
                    .defaultIfEmpty(false);
        }

        return categoryRepository.findOrgIdByCategoryId(categoryId)
                .flatMap(orgId -> {
                    if (orgId == null) {
                        return ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> ctx.getAuthentication().getAuthorities())
                                .map(auths -> auths.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                                        || tag.contains("list") || tag.contains("read"));
                    }
                    return hasPermission(orgId, tag);
                })
                .defaultIfEmpty(false);
    }

    private static boolean hasAuthority(
            org.springframework.security.core.Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
