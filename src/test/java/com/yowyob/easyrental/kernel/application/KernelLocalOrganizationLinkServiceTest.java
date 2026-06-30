package com.yowyob.easyrental.kernel.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelAuthClaims;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import com.yowyob.easyrental.kernel.security.KernelAuthenticationToken;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.dto.OrgUpdateDTO;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KernelLocalOrganizationLinkServiceTest {

    @Mock private OrganizationRepositoryPort organizationRepository;
    @Mock private SubscriptionPlanRepositoryPort planRepository;
    @Mock private SubscriptionUseCase subscriptionUseCase;
    @Mock private KernelOrganizationAdapter kernelOrganizationAdapter;
    @Mock private KernelOrganizationBootstrapService kernelOrganizationBootstrapService;
    @Mock private KernelClientProperties kernelProperties;

    private KernelLocalOrganizationLinkService linkService;

    @BeforeEach
    void setUp() {
        linkService = new KernelLocalOrganizationLinkService(
                organizationRepository,
                planRepository,
                subscriptionUseCase,
                kernelOrganizationAdapter,
                kernelOrganizationBootstrapService,
                kernelProperties);
        when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
    }

    @Test
    void shouldLinkLocalOrganizationFromJwtOidWithoutCreatingRemoteOrganization() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID kernelOrgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UserEntity owner = UserEntity.builder().id(ownerId).email("owner@test.com").fullname("Sahel Owner").build();
        OrgUpdateDTO request = new OrgUpdateDTO(
                "Sahel", "desc", "addr", "Douala", "00237", "Littoral",
                "699000000", "owner@test.com", "https://sahel.cm", "Africa/Douala",
                null, "RC123", "NIU123", false);
        KernelAuthClaims claims = new KernelAuthClaims(
                UUID.randomUUID().toString(),
                "owner@test.com",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(UUID.randomUUID()),
                List.of("hrm:onboarding:manage#ORGANIZATION:" + kernelOrgId),
                List.of("ORGANIZATION_ADMIN"));
        KernelAuthenticationToken auth = new KernelAuthenticationToken(claims, "token");
        KernelRequestContext kernelContext = KernelRequestContext.builder()
                .bearerToken(Optional.of("token"))
                .build();
        SubscriptionPlanEntity freePlan = SubscriptionPlanEntity.builder().id(planId).name("FREE").build();
        ObjectNode kernelOrg = new ObjectMapper().createObjectNode()
                .put("displayName", "Sahel")
                .put("governanceStatus", "APPROVED");

        when(organizationRepository.findByOwnerId(ownerId)).thenReturn(Mono.empty());
        when(organizationRepository.findByKernelOrganizationId(kernelOrgId)).thenReturn(Mono.empty());
        when(planRepository.findByName("FREE")).thenReturn(Mono.just(freePlan));
        when(kernelOrganizationAdapter.getOrganization(eq(kernelOrgId), any())).thenReturn(Mono.just(kernelOrg));
        when(organizationRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(kernelOrganizationBootstrapService.subscribeDefaultServices(eq(kernelOrgId), any()))
                .thenReturn(Mono.empty());
        when(subscriptionUseCase.createHistoryRecord(any(), eq("FREE"), eq(null))).thenReturn(Mono.empty());

        StepVerifier.create(linkService.ensureLocalOrganization(owner, request)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                        .contextWrite(ctx -> KernelContextHolder.withContext(ctx, kernelContext)))
                .expectNextMatches(org -> kernelOrgId.equals(org.getKernelOrganizationId())
                        && "Sahel".equals(org.getName()))
                .verifyComplete();

        verify(kernelOrganizationAdapter, never()).createOrganization(any(), any());
    }
}
