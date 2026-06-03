package com.yowyob.easyrental.shared.security;

import com.yowyob.easyrental.modules.agency.infrastructure.adapter.out.persistence.AgencyRepository;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence.UserRepository;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence.OrganizationRepository;
import com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.persistence.StaffRepository;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.CategoryRepository;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private AgencyRepository agencyRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private CategoryRepository categoryRepository;
    @InjectMocks private AccessControlService accessControlService;

    private UsernamePasswordAuthenticationToken auth(String email, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return new UsernamePasswordAuthenticationToken(email, null, authorities);
    }

    private UserEntity user(String email, String role) {
        return UserEntity.builder().id(UUID.randomUUID()).email(email).role(role).build();
    }

    @Test
    void shouldGrantPermissionToAdmin() {
        UUID orgId = UUID.randomUUID();
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Mono.just(user("admin@test.com", "ADMIN")));

        StepVerifier.create(accessControlService.hasPermission(orgId, "ANY")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth("admin@test.com", "ADMIN"))))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldGrantPermissionToOrganizationOwner() {
        UUID orgId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Mono.just(
                UserEntity.builder().id(ownerId).email("owner@test.com").role("ORGANIZATION").build()));
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(
                OrganizationEntity.builder().id(orgId).ownerId(ownerId).build()));

        StepVerifier.create(accessControlService.hasPermission(orgId.toString(), "UPDATE")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth("owner@test.com"))))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldDenyPermissionWhenOrganizationOwnerMismatch() {
        UUID orgId = UUID.randomUUID();
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Mono.just(
                UserEntity.builder().id(UUID.randomUUID()).email("owner@test.com").role("ORGANIZATION").build()));
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(
                OrganizationEntity.builder().id(orgId).ownerId(UUID.randomUUID()).build()));

        StepVerifier.create(accessControlService.hasPermission(orgId, "UPDATE")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth("owner@test.com"))))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldCheckStaffPermission() {
        UUID orgId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        when(userRepository.findByEmail("staff@test.com")).thenReturn(Mono.just(
                UserEntity.builder().id(staffId).email("staff@test.com").role("STAFF").build()));
        when(staffRepository.checkStaffPermission(staffId, orgId, "VEHICLE_CREATE")).thenReturn(Mono.just(true));

        StepVerifier.create(accessControlService.hasPermission(orgId, "VEHICLE_CREATE")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth("staff@test.com", "STAFF"))))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldDenyPermissionWhenNoSecurityContext() {
        StepVerifier.create(accessControlService.hasPermission(UUID.randomUUID(), "READ"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldCheckVehicleAccessViaOrganization() {
        UUID vehicleId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(vehicleRepository.findOrgIdByVehicleId(vehicleId)).thenReturn(Mono.just(orgId));
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Mono.just(
                UserEntity.builder().id(ownerId).email("owner@test.com").role("ORGANIZATION").build()));
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(
                OrganizationEntity.builder().id(orgId).ownerId(ownerId).build()));

        StepVerifier.create(accessControlService.canAccessVehicle(vehicleId.toString(), "READ")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth("owner@test.com"))))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldCheckAgencyAccessViaOrganization() {
        UUID agencyId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(agencyRepository.findOrgIdByAgencyId(agencyId)).thenReturn(Mono.just(orgId));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Mono.just(user("admin@test.com", "ADMIN")));

        StepVerifier.create(accessControlService.canAccessAgency(agencyId, "UPDATE")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth("admin@test.com", "ADMIN"))))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldCheckStaffMemberAccess() {
        UUID staffMemberId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(staffRepository.findOrgIdByStaffId(staffMemberId)).thenReturn(Mono.just(orgId));
        when(userRepository.findByEmail("staff@test.com")).thenReturn(Mono.just(
                UserEntity.builder().id(requesterId).email("staff@test.com").role("STAFF").build()));
        when(staffRepository.checkStaffPermission(requesterId, orgId, "STAFF_READ")).thenReturn(Mono.just(true));

        StepVerifier.create(accessControlService.canAccessStaffMember(staffMemberId, "STAFF_READ")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth("staff@test.com", "STAFF"))))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldCheckOrganizationCategoryAccess() {
        UUID categoryId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(categoryRepository.findOrgIdByCategoryId(categoryId)).thenReturn(Mono.just(orgId));
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Mono.just(
                UserEntity.builder().id(ownerId).email("owner@test.com").role("ORGANIZATION").build()));
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(
                OrganizationEntity.builder().id(orgId).ownerId(ownerId).build()));

        StepVerifier.create(accessControlService.canAccessCategory(categoryId.toString(), "UPDATE")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth("owner@test.com"))))
                .expectNext(true)
                .verifyComplete();
    }
}
