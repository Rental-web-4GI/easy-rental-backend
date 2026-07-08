package com.yowyob.easyrental.modules.subscription.application;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.dto.CreatePlanRequest;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionRemainingTimeDTO;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import com.yowyob.easyrental.shared.enums.PaymentMethod;
import com.yowyob.easyrental.shared.exception.ValidationException;
import com.yowyob.easyrental.modules.subscription.mapper.SubscriptionMapper;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionRepositoryPort;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionPaymentUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionUseCaseImpl implements SubscriptionUseCase {

        private final SubscriptionPlanRepositoryPort planRepository;
        private final SubscriptionRepositoryPort subscriptionRepository; // FIX : Utiliser le bon type ici
        private final OrganizationRepositoryPort organizationRepository;
        private final SubscriptionPaymentUseCase paymentService;
        private final ApplicationEventPublisher eventPublisher;
        private final SubscriptionMapper subscriptionMapper;

        @Transactional
        public Mono<Void> initializeDefaultSubscription(UUID organizationId) {
                return planRepository.findByName("FREE")
                                .switchIfEmpty(Mono.error(new RuntimeException("Plan FREE non configuré")))
                                .flatMap(plan -> organizationRepository.findById(Objects.requireNonNull(organizationId))
                                                .flatMap(org -> {
                                                        // 1. Mise à jour de l'état de l'organisation
                                                        org.setSubscriptionPlanId(plan.getId());
                                                        org.setSubscriptionExpiresAt(null);
                                                        org.setSubscriptionAutoRenew(false);

                                                        // 2. Création de l'enregistrement historique
                                                        SubscriptionEntity subRecord = SubscriptionEntity.builder()
                                                                        .id(UUID.randomUUID())
                                                                        .organizationId(organizationId)
                                                                        .planType(plan.getName())
                                                                        .status("ACTIVE")
                                                                        .startDate(LocalDateTime.now())
                                                                        .isNewRecord(true)
                                                                        .build();

                                                        // 3. On chaîne les deux sauvegardes pour qu'elles s'exécutent
                                                        return organizationRepository.save(org)
                                                                        .then(subscriptionRepository.save(Objects
                                                                                        .requireNonNull(subRecord)))
                                                                        .doOnSuccess(s -> log.info(
                                                                                "Historique souscription org {}",
                                                                                organizationId));
                                                }))
                                .then();
        }

        @Transactional
        public Mono<SubscriptionPlanEntity> upgradePlan(
                        UUID organizationId,
                        String planName,
                        PaymentMethod paymentMethod) {
                return planRepository.findByName(planName)
                                .switchIfEmpty(Mono.error(
                                                new ValidationException("Unknown subscription plan: " + planName)))
                                .flatMap(plan -> organizationRepository.findById(
                                                Objects.requireNonNull(organizationId))
                                                .switchIfEmpty(Mono.error(new ValidationException(
                                                        "Organization not found: " + organizationId)))
                                                .flatMap(org -> {
                                                        if ("FREE".equalsIgnoreCase(planName)) {
                                                                return applyFreePlan(org, organizationId, plan);
                                                        }
                                                        return resolvePayment(
                                                                        org.getEmail(), planName, plan,
                                                                        paymentMethod)
                                                                .flatMap(success -> {
                                                                        if (!Boolean.TRUE.equals(success)) {
                                                                                String paymentError =
                                                                                        "Payment failed for plan: "
                                                                                                + planName;
                                                                                return Mono.error(
                                                                                        new ValidationException(
                                                                                                paymentError));
                                                                        }
                                                                        return applyPaidPlan(
                                                                                org, organizationId, plan);
                                                                });
                                                }));
        }

        private Mono<SubscriptionPlanEntity> applyPaidPlan(
                        OrganizationEntity org,
                        UUID organizationId,
                        SubscriptionPlanEntity plan) {
                org.setSubscriptionPlanId(plan.getId());
                org.setSubscriptionAutoRenew(false);
                Integer durationDays = plan.getDurationDays();
                if (durationDays == null || durationDays <= 0) {
                        org.setSubscriptionExpiresAt(null);
                } else {
                        org.setSubscriptionExpiresAt(LocalDateTime.now().plusDays(durationDays));
                }

                SubscriptionEntity subRecord = SubscriptionEntity.builder()
                                .id(UUID.randomUUID())
                                .organizationId(organizationId)
                                .planType(plan.getName())
                                .status("ACTIVE")
                                .startDate(LocalDateTime.now())
                                .endDate(org.getSubscriptionExpiresAt())
                                .isNewRecord(true)
                                .build();

                return organizationRepository.save(org)
                                .then(subscriptionRepository.save(Objects.requireNonNull(subRecord)))
                                .thenReturn(plan);
        }

        private Mono<SubscriptionPlanEntity> applyFreePlan(
                        OrganizationEntity org,
                        UUID organizationId,
                        SubscriptionPlanEntity freePlan) {
                org.setSubscriptionPlanId(freePlan.getId());
                org.setSubscriptionExpiresAt(null);
                org.setSubscriptionAutoRenew(false);

                SubscriptionEntity subRecord = SubscriptionEntity.builder()
                                .id(UUID.randomUUID())
                                .organizationId(organizationId)
                                .planType(freePlan.getName())
                                .status("MANUAL_DOWNGRADE")
                                .startDate(LocalDateTime.now())
                                .isNewRecord(true)
                                .build();

                return organizationRepository.save(org)
                                .then(subscriptionRepository.save(Objects.requireNonNull(subRecord)))
                                .thenReturn(freePlan);
        }

        @Transactional
        public Mono<SubscriptionPlanEntity> adminAssignPlan(UUID organizationId, String planName) {
                return planRepository.findByName(planName)
                                .switchIfEmpty(Mono.error(
                                                new ValidationException("Unknown subscription plan: " + planName)))
                                .flatMap(plan -> organizationRepository.findById(
                                                Objects.requireNonNull(organizationId))
                                                .flatMap(org -> {
                                                        org.setSubscriptionPlanId(plan.getId());
                                                        Integer durationDays = plan.getDurationDays();
                                                        if (durationDays != null && durationDays > 0) {
                                                                org.setSubscriptionExpiresAt(
                                                                                LocalDateTime.now().plusDays(
                                                                                                durationDays));
                                                        } else {
                                                                org.setSubscriptionExpiresAt(null);
                                                        }
                                                        org.setSubscriptionAutoRenew(false);

                                                        SubscriptionEntity subRecord = SubscriptionEntity.builder()
                                                                        .id(UUID.randomUUID())
                                                                        .organizationId(organizationId)
                                                                        .planType(plan.getName())
                                                                        .status("ADMIN_ASSIGNED")
                                                                        .startDate(LocalDateTime.now())
                                                                        .endDate(org.getSubscriptionExpiresAt())
                                                                        .isNewRecord(true)
                                                                        .build();

                                                        return organizationRepository.save(org)
                                                                        .then(subscriptionRepository.save(
                                                                                        Objects.requireNonNull(
                                                                                                        subRecord)))
                                                                        .thenReturn(plan);
                                                }));
        }

        @Transactional
        public Mono<OrganizationEntity> checkAndDowngrade(OrganizationEntity org) {
                if (org.getSubscriptionExpiresAt() != null
                                && org.getSubscriptionExpiresAt().isBefore(LocalDateTime.now())) {
                        return planRepository.findByName("FREE")
                                        .flatMap(freePlan -> {
                                                        org.setSubscriptionPlanId(freePlan.getId());
                                                        org.setSubscriptionExpiresAt(null);
                                                        org.setSubscriptionAutoRenew(false);

                                                SubscriptionEntity history = SubscriptionEntity.builder()
                                                                .id(UUID.randomUUID())
                                                                .organizationId(org.getId())
                                                                .planType("FREE")
                                                                .status("AUTO_DOWNGRADE")
                                                                .startDate(LocalDateTime.now())
                                                                .isNewRecord(true)
                                                                .build();

                                                return subscriptionRepository.save(Objects.requireNonNull(history))
                                                                .then(organizationRepository.save(org));
                                        });
                }
                return Mono.just(org);
        }

        public Mono<SubscriptionRemainingTimeDTO> getRemainingTime(UUID orgId) {
                return organizationRepository.findById(Objects.requireNonNull(orgId))
                                .flatMap(this::checkAndDowngrade)
                                .map(org -> {
                                        if (org.getSubscriptionExpiresAt() == null) {
                                                return new SubscriptionRemainingTimeDTO(0, 0, 0, "Illimité (Plan FREE)",
                                                                true);
                                        }
                                        Duration d = Duration.between(LocalDateTime.now(),
                                                        org.getSubscriptionExpiresAt());
                                        return new SubscriptionRemainingTimeDTO(d.toDays(), d.toHoursPart(),
                                                        d.toMinutesPart(),
                                                        d.toDays() + " jours restants", false);
                                });
        }

        public Mono<SubscriptionEntity> createHistoryRecord(UUID organizationId, String planName,
                        LocalDateTime endDate) {
                SubscriptionEntity history = SubscriptionEntity.builder()
                                .id(UUID.randomUUID())
                                .organizationId(organizationId)
                                .planType(planName)
                                .status("ACTIVE")
                                .startDate(LocalDateTime.now())
                                .endDate(endDate)
                                .isNewRecord(true)
                                .build();

                return subscriptionRepository.save(Objects.requireNonNull(history));
        }

        // --- Dans SubscriptionUseCaseImpl.java ---

        /**
         * Active ou désactive le renouvellement automatique d'une organisation.
         */
        @Transactional
        public Mono<OrganizationEntity> toggleAutoRenew(UUID orgId, boolean autoRenew) {
                return organizationRepository.findById(Objects.requireNonNull(orgId))
                                .switchIfEmpty(Mono.error(new RuntimeException("Organisation non trouvée")))
                                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org
                                        .getSubscriptionPlanId()))
                                                .flatMap(plan -> {
                                                        // Optionnel : Empêcher la modification si c'est un plan FREE
                                                        if ("FREE".equalsIgnoreCase(plan.getName())) {
                                                                return Mono.error(new RuntimeException(
                                                                        "Auto-renew non applicable au plan FREE"));
                                                        }

                                                        org.setSubscriptionAutoRenew(autoRenew);
                                                        return organizationRepository.save(org);
                                                }))
                                .doOnSuccess(updated -> eventPublisher.publishEvent(new AuditEvent(
                                                "UPDATE_AUTO_RENEW",
                                                "SUBSCRIPTION",
                                                "Auto-renew " + autoRenew + " for " + updated.getName())));
        }

        @Override
        public Flux<SubscriptionPlanEntity> getAllPlans() {
                return planRepository.findAll();
        }

        @Override
        public Mono<SubscriptionPlanEntity> updatePlan(UUID id, SubscriptionPlanEntity planUpdate) {
                return planRepository.findById(Objects.requireNonNull(id, "Plan id is required"))
                                .flatMap(existingPlan -> {
                                        mergePlanFields(existingPlan, planUpdate);
                                        return planRepository.save(existingPlan);
                                })
                                .switchIfEmpty(Mono.error(new RuntimeException("Plan not found")));
        }

        @Override
        @Transactional
        public Mono<SubscriptionPlanEntity> createPlan(CreatePlanRequest request) {
                return planRepository.findByName(request.name())
                                .flatMap(existing -> Mono.<SubscriptionPlanEntity>error(
                                                new ValidationException("Plan name already exists: " + request.name())))
                                .switchIfEmpty(Mono.defer(() -> {
                                        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder()
                                                        .id(UUID.randomUUID())
                                                        .name(request.name())
                                                        .description(request.description())
                                                        .price(request.price())
                                                        .durationDays(request.durationDays())
                                                        .maxVehicles(defaultQuota(request.maxVehicles()))
                                                        .maxDrivers(defaultQuota(request.maxDrivers()))
                                                        .maxAgencies(defaultQuota(request.maxAgencies()))
                                                        .maxUsers(defaultQuota(request.maxUsers()))
                                                        .hasGeofencing(Boolean.TRUE.equals(request.hasGeofencing()))
                                                        .hasChat(Boolean.TRUE.equals(request.hasChat()))
                                                        .isNewRecord(true)
                                                        .build();
                                        return planRepository.save(plan);
                                }));
        }

        private Mono<Boolean> resolvePayment(
                        String email,
                        String planName,
                        SubscriptionPlanEntity plan,
                        PaymentMethod paymentMethod) {
                BigDecimal price = plan.getPrice() != null ? plan.getPrice() : BigDecimal.ZERO;
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                        return Mono.just(true);
                }
                return paymentService.processPayment(email, planName, price.doubleValue(), paymentMethod);
        }

        private void mergePlanFields(SubscriptionPlanEntity existing, SubscriptionPlanEntity update) {
                if (update.getName() != null) {
                        existing.setName(update.getName());
                }
                if (update.getDescription() != null) {
                        existing.setDescription(update.getDescription());
                }
                if (update.getPrice() != null) {
                        existing.setPrice(update.getPrice());
                }
                if (update.getDurationDays() != null) {
                        existing.setDurationDays(update.getDurationDays());
                }
                if (update.getMaxVehicles() != null) {
                        existing.setMaxVehicles(update.getMaxVehicles());
                }
                if (update.getMaxDrivers() != null) {
                        existing.setMaxDrivers(update.getMaxDrivers());
                }
                if (update.getMaxAgencies() != null) {
                        existing.setMaxAgencies(update.getMaxAgencies());
                }
                if (update.getMaxUsers() != null) {
                        existing.setMaxUsers(update.getMaxUsers());
                }
                if (update.getHasGeofencing() != null) {
                        existing.setHasGeofencing(update.getHasGeofencing());
                }
                if (update.getHasChat() != null) {
                        existing.setHasChat(update.getHasChat());
                }
        }

        private int defaultQuota(Integer value) {
                return value != null ? value : 0;
        }

        @Override
        public Mono<SubscriptionResponseDTO> getOrgSubscriptionStatus(UUID orgId) {
                return organizationRepository.findById(Objects.requireNonNull(orgId))
                                .flatMap(this::checkAndDowngrade)
                                .flatMap(this::buildSubscriptionResponse);
        }

        @Override
        public Mono<Long> processExpiredSubscriptions() {
                return organizationRepository.findAllExpiredBefore(LocalDateTime.now())
                                .flatMap(this::checkAndDowngrade)
                                .count();
        }

        @Override
        public Mono<SubscriptionResponseDTO> buildSubscriptionResponse(OrganizationEntity org) {
                return planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                                .map(plan -> subscriptionMapper.toResponseDTO(org, plan));
        }
}
