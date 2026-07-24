package com.yowyob.easyrental.modules.organization.application;

import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.application.KernelBusinessActorProvisioningService;
import com.yowyob.easyrental.kernel.application.KernelLocalOrganizationLinkService;
import com.yowyob.easyrental.kernel.application.KernelOrganizationBootstrapService;
import com.yowyob.easyrental.kernel.application.KernelOwnerAssignmentService;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.media.domain.port.in.MediaUseCase;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.mapper.OrgMapper;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationUseCaseImplUpgradeTest {

    @Mock private OrganizationRepositoryPort orgRepository;
    @Mock private SubscriptionPlanRepositoryPort planRepository;
    @Mock private OrgMapper orgMapper;
    @Mock private MediaUseCase mediaService;
    @Mock private UserRepositoryPort userRepository;
    @Mock private SubscriptionUseCase subscriptionUseCase;
    @Mock private KernelClientProperties kernelProperties;
    @Mock private KernelOrganizationAdapter kernelOrganizationAdapter;
    @Mock private KernelOrganizationBootstrapService kernelOrganizationBootstrapService;
    @Mock private KernelLocalOrganizationLinkService kernelLocalOrganizationLinkService;
    @Mock private KernelOwnerAssignmentService kernelOwnerAssignmentService;
    @Mock private KernelBusinessActorProvisioningService kernelBusinessActorProvisioningService;
    @Mock private AgencyRepositoryPort agencyRepositoryPort;

    private OrganizationUseCaseImpl useCase;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new OrganizationUseCaseImpl(
                orgRepository,
                planRepository,
                orgMapper,
                mediaService,
                userRepository,
                subscriptionUseCase,
                kernelProperties,
                kernelOrganizationAdapter,
                kernelOrganizationBootstrapService,
                kernelLocalOrganizationLinkService,
                kernelOwnerAssignmentService,
                kernelBusinessActorProvisioningService,
                agencyRepositoryPort);
    }

    @Test
    void upgradeToCompany_changesAccountType() {
        OrganizationEntity freelance = OrganizationEntity.builder()
                .id(UUID.randomUUID()).accountType("FREELANCE").ownerId(userId).build();
        when(orgRepository.findByOwnerId(userId)).thenReturn(Mono.just(freelance));
        when(orgRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.upgradeToCompany(userId))
                .expectNextMatches(o -> "COMPANY".equals(o.getAccountType()))
                .verifyComplete();
    }

    @Test
    void upgradeToCompany_alreadyCompany_isIdempotent() {
        OrganizationEntity company = OrganizationEntity.builder()
                .id(UUID.randomUUID()).accountType("COMPANY").ownerId(userId).build();
        when(orgRepository.findByOwnerId(userId)).thenReturn(Mono.just(company));

        StepVerifier.create(useCase.upgradeToCompany(userId))
                .expectNextMatches(o -> "COMPANY".equals(o.getAccountType()))
                .verifyComplete();

        verify(orgRepository, never()).save(any());
    }
}
