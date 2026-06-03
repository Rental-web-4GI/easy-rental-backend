package com.yowyob.easyrental.shared.security;

import com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence.UserRepository;
import com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.persistence.StaffRepository;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.CategoryRepository;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.VehicleRepository;
import com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence.OrganizationRepository;
import com.yowyob.easyrental.modules.agency.infrastructure.adapter.out.persistence.AgencyRepository; // Ajouté
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@Service("rbac")
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final VehicleRepository vehicleRepository;
    private final AgencyRepository agencyRepository;
    private final OrganizationRepository organizationRepository;
    private final CategoryRepository categoryRepository;

    public Mono<Boolean> hasPermission(Object orgIdObj, String permissionTag) {
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
        UUID vehicleId = (vehicleIdObj instanceof String) ? UUID.fromString((String) vehicleIdObj)
                : (UUID) vehicleIdObj;
        return vehicleRepository.findOrgIdByVehicleId(vehicleId)
                .flatMap(orgId -> hasPermission(orgId, tag));
    }

    public Mono<Boolean> canAccessAgency(Object agencyIdObj, String tag) {
        UUID agencyId = (agencyIdObj instanceof String) ? UUID.fromString((String) agencyIdObj) : (UUID) agencyIdObj;
        return agencyRepository.findOrgIdByAgencyId(agencyId)
                .flatMap(orgId -> hasPermission(orgId, tag));
    }

    public Mono<Boolean> canAccessStaffMember(Object staffIdObj, String tag) {
        UUID staffId = (staffIdObj instanceof String) ? UUID.fromString((String) staffIdObj) : (UUID) staffIdObj;
        return staffRepository.findOrgIdByStaffId(staffId)
                .flatMap(orgId -> hasPermission(orgId, tag));
    }
    // Dans AccessControlService.java

    public Mono<Boolean> canAccessCategory(Object categoryIdObj, String tag) {
        UUID categoryId = (categoryIdObj instanceof String) ? UUID.fromString((String) categoryIdObj)
                : (UUID) categoryIdObj;

        return categoryRepository.findOrgIdByCategoryId(categoryId)
                .flatMap(orgId -> {
                    // Si l'orgId est NULL, c'est une catégorie système
                    if (orgId == null) {
                        // Seul l'ADMIN peut modifier/supprimer du système
                        // Les autres peuvent seulement "lire" (le tag commence par 'list' ou 'read')
                        return ReactiveSecurityContextHolder.getContext()
                                .map(ctx -> ctx.getAuthentication().getAuthorities())
                                .map(auths -> auths.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                                        || tag.contains("list") || tag.contains("read"));
                    }
                    // Sinon, on applique la logique standard de l'organisation
                    return hasPermission(orgId, tag);
                })
                .defaultIfEmpty(false);
    }
}