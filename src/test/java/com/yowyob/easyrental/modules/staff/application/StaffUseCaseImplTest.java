package com.yowyob.easyrental.modules.staff.application;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.in.OrganizationUseCase;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.poste.domain.port.in.PosteUseCase;
import com.yowyob.easyrental.modules.poste.dto.PosteResponseDTO;
import com.yowyob.easyrental.modules.staff.domain.port.out.StaffRepositoryPort;
import com.yowyob.easyrental.modules.staff.dto.StaffRequestDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffResponseDTO;
import com.yowyob.easyrental.modules.staff.mapper.StaffMapper;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.staff.dto.StaffUpdateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffUseCaseImplTest {

    @Mock private StaffRepositoryPort staffRepository;
    @Mock private OrganizationRepositoryPort organizationRepository;
    @Mock private SubscriptionPlanRepositoryPort planRepository;
    @Mock private OrganizationUseCase organizationService;
    @Mock private AgencyRepositoryPort agencyRepository;
    @Mock private PosteUseCase posteService;
    @Mock private StaffMapper staffMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private StaffUseCaseImpl staffUseCase;

    @Test
    void shouldAddStaffToOrganization() {
        UUID orgId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        UUID posteId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        StaffRequestDTO request = new StaffRequestDTO("John", "Doe", "new-staff@test.com", agencyId, posteId);

        when(staffRepository.findByEmail("new-staff@test.com")).thenReturn(Mono.empty());
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(
                OrganizationEntity.builder().id(orgId).subscriptionPlanId(planId).currentUsers(1).build()));
        when(planRepository.findById(planId)).thenReturn(Mono.just(
                SubscriptionPlanEntity.builder().id(planId).maxUsers(5).build()));
        when(passwordEncoder.encode("motdepasse")).thenReturn("encoded-password");
        when(staffRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(organizationService.updateStaffCounter(orgId, 1)).thenReturn(Mono.empty());
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(
                AgencyEntity.builder().id(agencyId).totalPersonnel(2).build()));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(posteService.getPosteById(posteId)).thenReturn(Mono.just(mock(PosteResponseDTO.class)));
        when(staffMapper.toDto(any(), any())).thenReturn(mock(StaffResponseDTO.class));

        StepVerifier.create(staffUseCase.addStaffToOrganization(orgId, request))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenStaffQuotaReached() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        StaffRequestDTO request = new StaffRequestDTO("John", "Doe", "quota@test.com", UUID.randomUUID(), UUID.randomUUID());

        when(staffRepository.findByEmail("quota@test.com")).thenReturn(Mono.empty());
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(
                OrganizationEntity.builder().id(orgId).subscriptionPlanId(planId).currentUsers(5).build()));
        when(planRepository.findById(planId)).thenReturn(Mono.just(
                SubscriptionPlanEntity.builder().id(planId).maxUsers(5).build()));

        StepVerifier.create(staffUseCase.addStaffToOrganization(orgId, request))
                .expectErrorMatches(e -> e.getMessage().contains("Quota atteint"))
                .verify();
    }

    @Test
    void shouldReturnErrorWhenEmailAlreadyUsed() {
        UUID orgId = UUID.randomUUID();
        StaffRequestDTO request = new StaffRequestDTO("John", "Doe", "staff@test.com", UUID.randomUUID(), UUID.randomUUID());
        when(staffRepository.findByEmail("staff@test.com"))
                .thenReturn(Mono.just(UserEntity.builder().id(UUID.randomUUID()).email("staff@test.com").build()));

        StepVerifier.create(staffUseCase.addStaffToOrganization(orgId, request))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldGetStaffById() {
        UUID id = UUID.randomUUID();
        UUID posteId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(id).posteId(posteId).firstname("Jane").build();
        when(staffRepository.findById(id)).thenReturn(Mono.just(user));
        when(posteService.getPosteById(posteId)).thenReturn(Mono.just(mock(PosteResponseDTO.class)));
        when(staffMapper.toDto(any(), any())).thenReturn(mock(StaffResponseDTO.class));

        StepVerifier.create(staffUseCase.getStaffById(id))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetStaffByOrganization() {
        UUID orgId = UUID.randomUUID();
        UUID posteId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).posteId(posteId).build();
        when(staffRepository.findAllStaffByOrganizationId(orgId)).thenReturn(Flux.just(user));
        when(posteService.getPosteById(posteId)).thenReturn(Mono.just(mock(PosteResponseDTO.class)));
        when(staffMapper.toDto(any(), any())).thenReturn(mock(StaffResponseDTO.class));

        StepVerifier.create(staffUseCase.getStaffByOrganization(orgId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetStaffByAgency() {
        UUID agencyId = UUID.randomUUID();
        UUID posteId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).posteId(posteId).build();
        when(staffRepository.findAllStaffByAgencyId(agencyId)).thenReturn(Flux.just(user));
        when(posteService.getPosteById(posteId)).thenReturn(Mono.just(mock(PosteResponseDTO.class)));
        when(staffMapper.toDto(any(), any())).thenReturn(mock(StaffResponseDTO.class));

        StepVerifier.create(staffUseCase.getStaffByAgency(agencyId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldDeleteStaff() {
        UUID id = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(id).organizationId(orgId).agencyId(agencyId).build();
        when(staffRepository.findById(id)).thenReturn(Mono.just(user));
        when(staffRepository.delete(user)).thenReturn(Mono.empty());
        when(organizationService.updateStaffCounter(orgId, -1)).thenReturn(Mono.empty());
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(
                AgencyEntity.builder().id(agencyId).totalPersonnel(1).build()));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(staffUseCase.deleteStaff(id))
                .verifyComplete();
    }

    @Test
    void shouldUpdateStaff() {
        UUID staffId = UUID.randomUUID();
        UUID posteId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(staffId).posteId(posteId)
                .agencyId(UUID.randomUUID()).firstname("Old").lastname("Name").build();
        StaffUpdateDTO update = new StaffUpdateDTO("New", "Name", null, posteId, "ACTIVE");
        when(staffRepository.findById(staffId)).thenReturn(Mono.just(user));
        when(staffRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(posteService.getPosteById(posteId)).thenReturn(Mono.just(mock(PosteResponseDTO.class)));
        when(staffMapper.toDto(any(), any())).thenReturn(mock(StaffResponseDTO.class));

        StepVerifier.create(staffUseCase.updateStaff(staffId, update))
                .expectNextCount(1)
                .verifyComplete();
    }
}
