package com.yowyob.easyrental.modules.subscription.application;

import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionPaymentUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import com.yowyob.easyrental.modules.subscription.mapper.SubscriptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionUseCaseImplTest {

    @Mock
    private SubscriptionPlanRepositoryPort planRepository;
    @Mock
    private SubscriptionRepositoryPort subscriptionRepository;
    @Mock
    private OrganizationRepositoryPort organizationRepository;
    @Mock
    private SubscriptionPaymentUseCase paymentService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private SubscriptionMapper subscriptionMapper;

    @InjectMocks
    private SubscriptionUseCaseImpl subscriptionUseCase;

    @Test
    void shouldInitializeDefaultSubscription() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        SubscriptionPlanEntity free = SubscriptionPlanEntity.builder().id(planId).name("FREE").build();
        OrganizationEntity org = OrganizationEntity.builder().id(orgId).subscriptionPlanId(planId).build();

        when(planRepository.findByName("FREE")).thenReturn(Mono.just(free));
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(org));
        when(organizationRepository.save(any())).thenReturn(Mono.just(org));
        when(subscriptionRepository.save(any())).thenReturn(Mono.just(SubscriptionEntity.builder().id(UUID.randomUUID()).build()));

        StepVerifier.create(subscriptionUseCase.initializeDefaultSubscription(orgId))
                .verifyComplete();
    }

    @Test
    void shouldGetAllPlans() {
        when(planRepository.findAll()).thenReturn(Flux.just(
                SubscriptionPlanEntity.builder().id(UUID.randomUUID()).name("FREE").build()));

        StepVerifier.create(subscriptionUseCase.getAllPlans())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldCreateHistoryRecord() {
        UUID orgId = UUID.randomUUID();
        when(subscriptionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(subscriptionUseCase.createHistoryRecord(orgId, "FREE", null))
                .expectNextMatches(s -> s.getPlanType().equals("FREE"))
                .verifyComplete();
    }

    @Test
    void shouldGetRemainingTimeForFreePlan() {
        UUID orgId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder()
                .id(orgId).subscriptionExpiresAt(null).build();
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(org));

        StepVerifier.create(subscriptionUseCase.getRemainingTime(orgId))
                .expectNextMatches(dto -> dto.isInfinite())
                .verifyComplete();
    }

    @Test
    void shouldGetOrgSubscriptionStatus() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder()
                .id(orgId).subscriptionPlanId(planId).subscriptionExpiresAt(null).build();
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder().id(planId).name("FREE").price(BigDecimal.ZERO).build();
        SubscriptionResponseDTO response = new SubscriptionResponseDTO(
                "FREE", "Free plan", BigDecimal.ZERO, 30, 10, 5, null, false);

        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(org));
        when(planRepository.findById(planId)).thenReturn(Mono.just(plan));
        when(subscriptionMapper.toResponseDTO(org, plan)).thenReturn(response);

        StepVerifier.create(subscriptionUseCase.getOrgSubscriptionStatus(orgId))
                .expectNextMatches(dto -> dto.planName().equals("FREE"))
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenToggleAutoRenewOnFreePlan() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder().id(orgId).subscriptionPlanId(planId).build();
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder().id(planId).name("FREE").build();

        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(org));
        when(planRepository.findById(planId)).thenReturn(Mono.just(plan));

        StepVerifier.create(subscriptionUseCase.toggleAutoRenew(orgId, true))
                .expectErrorMatches(e -> e.getMessage().contains("FREE"))
                .verify();
    }

    @Test
    void shouldDowngradeExpiredSubscription() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder()
                .id(orgId)
                .subscriptionPlanId(planId)
                .subscriptionExpiresAt(LocalDateTime.now().minusDays(1))
                .build();
        SubscriptionPlanEntity free = SubscriptionPlanEntity.builder().id(UUID.randomUUID()).name("FREE").build();

        when(planRepository.findByName("FREE")).thenReturn(Mono.just(free));
        when(subscriptionRepository.save(any())).thenReturn(Mono.just(SubscriptionEntity.builder().id(UUID.randomUUID()).build()));
        when(organizationRepository.save(any())).thenReturn(Mono.just(org));

        StepVerifier.create(subscriptionUseCase.checkAndDowngrade(org))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldUpgradePlan() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder()
                .id(orgId).email("org@test.com").subscriptionPlanId(planId).build();
        SubscriptionPlanEntity proPlan = SubscriptionPlanEntity.builder()
                .id(planId).name("PRO").price(BigDecimal.valueOf(5000)).durationDays(30).build();

        when(planRepository.findByName("PRO")).thenReturn(Mono.just(proPlan));
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(org));
        when(paymentService.processPayment("org@test.com", "PRO", 5000.0)).thenReturn(Mono.just(true));
        when(organizationRepository.save(any())).thenReturn(Mono.just(org));
        when(subscriptionRepository.save(any())).thenReturn(Mono.just(SubscriptionEntity.builder().id(UUID.randomUUID()).build()));

        StepVerifier.create(subscriptionUseCase.upgradePlan(orgId, "PRO"))
                .expectNextMatches(p -> p.getName().equals("PRO"))
                .verifyComplete();
    }

    @Test
    void shouldUpdatePlan() {
        UUID planId = UUID.randomUUID();
        SubscriptionPlanEntity existing = SubscriptionPlanEntity.builder().id(planId).name("PRO").build();
        SubscriptionPlanEntity update = SubscriptionPlanEntity.builder().name("PRO_PLUS").price(BigDecimal.TEN).build();

        when(planRepository.findById(planId)).thenReturn(Mono.just(existing));
        when(planRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(subscriptionUseCase.updatePlan(planId, update))
                .expectNextMatches(p -> p.getName().equals("PRO_PLUS"))
                .verifyComplete();
    }

    @Test
    void shouldBuildSubscriptionResponse() {
        UUID planId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder()
                .id(UUID.randomUUID()).subscriptionPlanId(planId).build();
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder().id(planId).name("PRO").build();
        SubscriptionResponseDTO response = new SubscriptionResponseDTO(
                "PRO", "Pro plan", BigDecimal.TEN, 30, 10, 5, null, false);

        when(planRepository.findById(planId)).thenReturn(Mono.just(plan));
        when(subscriptionMapper.toResponseDTO(org, plan)).thenReturn(response);

        StepVerifier.create(subscriptionUseCase.buildSubscriptionResponse(org))
                .expectNextMatches(dto -> dto.planName().equals("PRO"))
                .verifyComplete();
    }
}
