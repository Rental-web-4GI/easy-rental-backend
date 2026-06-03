package com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.staff.domain.port.out.StaffRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StaffRepositoryAdapter implements StaffRepositoryPort {

    private final StaffRepository staffRepository;

    @Override
    public Mono<UserEntity> findById(UUID id) {
        return staffRepository.findById(id);
    }

    @Override
    public Mono<UserEntity> save(UserEntity user) {
        return staffRepository.save(user);
    }

    @Override
    public Mono<Void> delete(UserEntity user) {
        return staffRepository.delete(user);
    }

    @Override
    public Mono<UserEntity> findByIdAndOrganizationId(UUID id, UUID organizationId) {
        return staffRepository.findByIdAndOrganizationId(id, organizationId);
    }

    @Override
    public Flux<UserEntity> findAllByAgencyId(UUID agencyId) {
        return staffRepository.findAllByAgencyId(agencyId);
    }

    @Override
    public Flux<UserEntity> findAllByOrganizationId(UUID organizationId) {
        return staffRepository.findAllByOrganizationId(organizationId);
    }

    @Override
    public Flux<UserEntity> findAllByOrganizationIdAndRoleNot(UUID organizationId, String role) {
        return staffRepository.findAllByOrganizationIdAndRoleNot(organizationId, role);
    }

    @Override
    public Mono<UserEntity> findByEmail(String email) {
        return staffRepository.findByEmail(email);
    }

    @Override
    public Flux<UserEntity> findAllStaffByOrganizationId(UUID organizationId) {
        return staffRepository.findAllStaffByOrganizationId(organizationId);
    }

    @Override
    public Flux<UserEntity> findAllStaffByAgencyId(UUID agencyId) {
        return staffRepository.findAllStaffByAgencyId(agencyId);
    }

    @Override
    public Mono<UUID> findOrgIdByStaffId(UUID staffId) {
        return staffRepository.findOrgIdByStaffId(staffId);
    }

    @Override
    public Mono<Boolean> checkStaffPermission(UUID id, UUID orgId, String permissionTag) {
        return staffRepository.checkStaffPermission(id, orgId, permissionTag);
    }
}
