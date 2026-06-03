package com.yowyob.easyrental.modules.staff.domain.port.out;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for staff persistence.
 */
public interface StaffRepositoryPort {

    Mono<UserEntity> findById(UUID id);

    Mono<UserEntity> save(UserEntity user);

    Mono<Void> delete(UserEntity user);

    Mono<UserEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Flux<UserEntity> findAllByAgencyId(UUID agencyId);

    Flux<UserEntity> findAllByOrganizationId(UUID organizationId);

    Flux<UserEntity> findAllByOrganizationIdAndRoleNot(UUID organizationId, String role);

    Mono<UserEntity> findByEmail(String email);

    Flux<UserEntity> findAllStaffByOrganizationId(UUID organizationId);

    Flux<UserEntity> findAllStaffByAgencyId(UUID agencyId);

    Mono<UUID> findOrgIdByStaffId(UUID staffId);

    Mono<Boolean> checkStaffPermission(UUID id, UUID orgId, String permissionTag);
}
