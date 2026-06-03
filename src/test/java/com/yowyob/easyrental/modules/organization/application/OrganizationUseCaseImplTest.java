package com.yowyob.easyrental.modules.organization.application;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.media.domain.port.in.MediaUseCase;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.dto.OrgResponseDTO;
import com.yowyob.easyrental.modules.organization.dto.OrgUpdateDTO;
import com.yowyob.easyrental.modules.organization.mapper.OrgMapper;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationUseCaseImplTest {

    @Mock private OrganizationRepositoryPort organizationRepository;
    @Mock private SubscriptionPlanRepositoryPort planRepository;
    @Mock private OrgMapper orgMapper;
    @Mock private MediaUseCase mediaService;
    @Mock private UserRepositoryPort userRepository;
    @Mock private SubscriptionUseCase subscriptionUseCase;
    @InjectMocks private OrganizationUseCaseImpl organizationUseCase;

    @Test
    void shouldGetOrganization() {
        UUID id = UUID.randomUUID();
        OrganizationEntity entity = OrganizationEntity.builder().id(id).name("Org").build();
        OrgResponseDTO dto = mock(OrgResponseDTO.class);
        when(organizationRepository.findById(id)).thenReturn(Mono.just(entity));
        when(orgMapper.toDto(entity)).thenReturn(dto);

        StepVerifier.create(organizationUseCase.getOrganization(id))
                .expectNext(dto)
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenOrganizationNotFound() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(organizationUseCase.getOrganization(id))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldValidateQuotaForVehicle() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        OrganizationEntity organization = OrganizationEntity.builder()
                .id(orgId).subscriptionPlanId(planId).currentVehicles(1).build();
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder().id(planId).maxVehicles(5).build();
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(organization));
        when(planRepository.findById(planId)).thenReturn(Mono.just(plan));

        StepVerifier.create(organizationUseCase.validateQuota(orgId, "VEHICLE"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldValidateQuotaForAgencyDriverAndStaff() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        OrganizationEntity organization = OrganizationEntity.builder()
                .id(orgId).subscriptionPlanId(planId)
                .currentAgencies(1).currentDrivers(2).currentUsers(3).build();
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder()
                .id(planId).maxAgencies(5).maxDrivers(5).maxUsers(5).build();
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(organization));
        when(planRepository.findById(planId)).thenReturn(Mono.just(plan));

        StepVerifier.create(organizationUseCase.validateQuota(orgId, "AGENCY"))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(organizationUseCase.validateQuota(orgId, "DRIVER"))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(organizationUseCase.validateQuota(orgId, "STAFF"))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(organizationUseCase.validateQuota(orgId, "UNKNOWN"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldUpdateOrganizationWithAllFields() {
        UUID id = UUID.randomUUID();
        OrganizationEntity entity = OrganizationEntity.builder().id(id).name("Old").build();
        OrgResponseDTO dto = mock(OrgResponseDTO.class);
        when(organizationRepository.findById(id)).thenReturn(Mono.just(entity));
        when(organizationRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orgMapper.toDto(any())).thenReturn(dto);

        OrgUpdateDTO updateDto = mock(OrgUpdateDTO.class);
        when(updateDto.name()).thenReturn("New");
        when(updateDto.description()).thenReturn("Desc");
        when(updateDto.address()).thenReturn("Addr");
        when(updateDto.city()).thenReturn("City");
        when(updateDto.postalCode()).thenReturn("12345");
        when(updateDto.region()).thenReturn("Region");
        when(updateDto.phone()).thenReturn("690000000");
        when(updateDto.email()).thenReturn("org@test.com");
        when(updateDto.website()).thenReturn("https://org.com");
        when(updateDto.timezone()).thenReturn("Africa/Douala");
        when(updateDto.logoUrl()).thenReturn("logo.png");
        when(updateDto.registrationNumber()).thenReturn("RC123");
        when(updateDto.taxNumber()).thenReturn("NIF456");
        StepVerifier.create(organizationUseCase.updateOrganization(id, updateDto))
                .expectNext(dto)
                .verifyComplete();
    }

    @Test
    void shouldListAllOrganizations() {
        OrganizationEntity entity = OrganizationEntity.builder().id(UUID.randomUUID()).build();
        when(organizationRepository.findAll()).thenReturn(Flux.just(entity));
        when(orgMapper.toDto(entity)).thenReturn(mock(OrgResponseDTO.class));

        StepVerifier.create(organizationUseCase.getAllOrganizations())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldUpdateVehicleCounter() {
        UUID orgId = UUID.randomUUID();
        OrganizationEntity organization = OrganizationEntity.builder().id(orgId).currentVehicles(2).build();
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(organization));
        when(organizationRepository.save(any())).thenReturn(Mono.just(organization));

        StepVerifier.create(organizationUseCase.updateVehicleCounter(orgId, 1))
                .verifyComplete();
    }

    @Test
    void shouldUpdateAgencyCounter() {
        UUID orgId = UUID.randomUUID();
        OrganizationEntity organization = OrganizationEntity.builder().id(orgId).currentAgencies(1).build();
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(organization));
        when(organizationRepository.save(any())).thenReturn(Mono.just(organization));

        StepVerifier.create(organizationUseCase.updateAgencyCounter(orgId, 1))
                .verifyComplete();
    }

    @Test
    void shouldUpdateStaffCounter() {
        UUID orgId = UUID.randomUUID();
        OrganizationEntity organization = OrganizationEntity.builder().id(orgId).currentUsers(2).build();
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(organization));
        when(organizationRepository.save(any())).thenReturn(Mono.just(organization));

        StepVerifier.create(organizationUseCase.updateStaffCounter(orgId, 1))
                .verifyComplete();
    }

    @Test
    void shouldUpdateDriverCounter() {
        UUID orgId = UUID.randomUUID();
        OrganizationEntity organization = OrganizationEntity.builder().id(orgId).currentDrivers(1).build();
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(organization));
        when(organizationRepository.save(any())).thenReturn(Mono.just(organization));

        StepVerifier.create(organizationUseCase.updateDriverCounter(orgId, 1))
                .verifyComplete();
    }

    @Test
    void shouldUpdateOrganizationWithMedia() {
        UUID id = UUID.randomUUID();
        OrganizationEntity entity = OrganizationEntity.builder().id(id).name("Org").build();
        OrgResponseDTO dto = mock(OrgResponseDTO.class);
        OrgUpdateDTO updateDto = mock(OrgUpdateDTO.class);
        when(organizationRepository.findById(id)).thenReturn(Mono.just(entity));
        when(organizationRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(orgMapper.toDto(any())).thenReturn(dto);

        StepVerifier.create(organizationUseCase.updateOrganizationWithMedia(id, updateDto, null, null))
                .expectNext(dto)
                .verifyComplete();
    }

    @Test
    void shouldGetCurrentOrgAndUser() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("owner@test.com").build();
        OrganizationEntity org = OrganizationEntity.builder().id(UUID.randomUUID()).name("MyOrg").build();
        OrgResponseDTO dto = mock(OrgResponseDTO.class);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Mono.just(user));
        when(organizationRepository.findByOwnerId(user.getId())).thenReturn(Mono.just(org));
        when(orgMapper.toDto(org)).thenReturn(dto);
        var auth = new UsernamePasswordAuthenticationToken("owner@test.com", null);

        StepVerifier.create(organizationUseCase.getCurrentOrgAndUser()
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectNextMatches(r -> r.user().getEmail().equals("owner@test.com"))
                .verifyComplete();
    }

    @Test
    void shouldGetOrganizationsByPlan() {
        UUID planId = UUID.randomUUID();
        OrganizationEntity entity = OrganizationEntity.builder().id(UUID.randomUUID()).subscriptionPlanId(planId).build();
        when(organizationRepository.findAllBySubscriptionPlanId(planId)).thenReturn(Flux.just(entity));
        when(orgMapper.toDto(entity)).thenReturn(mock(OrgResponseDTO.class));

        StepVerifier.create(organizationUseCase.getOrganizationsByPlan(planId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldToggleAutoRenewWithResponse() {
        UUID orgId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder().id(orgId).build();
        SubscriptionResponseDTO response = new SubscriptionResponseDTO(
                "PRO", "Pro plan", BigDecimal.TEN, 30, 10, 5, null, true);
        when(subscriptionUseCase.toggleAutoRenew(orgId, true)).thenReturn(Mono.just(org));
        when(subscriptionUseCase.buildSubscriptionResponse(org)).thenReturn(Mono.just(response));

        StepVerifier.create(organizationUseCase.toggleAutoRenewWithResponse(orgId, true))
                .expectNextMatches(r -> r.planName().equals("PRO"))
                .verifyComplete();
    }

    @Test
    void shouldGetOrgSubscriptionStatus() {
        UUID orgId = UUID.randomUUID();
        SubscriptionResponseDTO response = new SubscriptionResponseDTO(
                "FREE", "Free", BigDecimal.ZERO, 30, 10, 5, null, false);
        when(subscriptionUseCase.getOrgSubscriptionStatus(orgId)).thenReturn(Mono.just(response));

        StepVerifier.create(organizationUseCase.getOrgSubscriptionStatus(orgId))
                .expectNextMatches(r -> r.planName().equals("FREE"))
                .verifyComplete();
    }

    @Test
    void shouldUpgradePlanWithResponse() {
        UUID orgId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder().id(orgId).build();
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder().id(UUID.randomUUID()).name("PRO").build();
        SubscriptionResponseDTO response = new SubscriptionResponseDTO(
                "PRO", "Pro", BigDecimal.TEN, 30, 10, 5, null, false);
        when(subscriptionUseCase.upgradePlan(orgId, "PRO")).thenReturn(Mono.just(plan));
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(org));
        when(subscriptionUseCase.buildSubscriptionResponse(org)).thenReturn(Mono.just(response));

        StepVerifier.create(organizationUseCase.upgradePlanWithResponse(orgId, "PRO"))
                .expectNextMatches(r -> r.planName().equals("PRO"))
                .verifyComplete();
    }
}
