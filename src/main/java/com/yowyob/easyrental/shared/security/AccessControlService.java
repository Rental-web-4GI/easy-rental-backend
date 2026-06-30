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
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
