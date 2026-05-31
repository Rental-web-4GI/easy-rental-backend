package com.yowyob.easyrental.modules.subscription.application;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionRemainingTimeDTO;
import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionPlanRepository;
import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionRepository;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence.OrganizationRepository;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionUseCaseImpl implements SubscriptionUseCase {

        private final SubscriptionPlanRepository planRepository;
        private final SubscriptionRepository subscriptionRepository; // FIX : Utiliser le bon type ici
        private final OrganizationRepository organizationRepository;
        private final SubscriptionPaymentUseCaseImpl paymentService;
        private final ApplicationEventPublisher eventPublisher;

        @Transactional
        public Mono<Void> initializeDefaultSubscription(UUID organizationId) {
                return planRepository.findByName("FREE")
                                .switchIfEmpty(Mono.error(new RuntimeException("Plan FREE non configuré")))
                                .flatMap(plan -> organizationRepository.findById(Objects.requireNonNull(organizationId))
                                                .flatMap(org -> {
                                                        // 1. Mise à jour de l'état de l'organisation
                                                        org.setSubscriptionPlanId(plan.getId());
                                                        org.setSubscriptionExpiresAt(null);
                                                        org.setSubscriptionAutoRenew(true);

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
                                                                                        "✅ Historique de souscription créé pour l'org {}",
                                                                                        organizationId));
                                                }))
                                .then();
        }

        @Transactional
        public Mono<SubscriptionPlanEntity> upgradePlan(UUID organizationId, String planName) {
                return planRepository.findByName(planName)
                                .flatMap(plan -> organizationRepository.findById(Objects.requireNonNull(organizationId))
                                                .flatMap(org -> paymentService
                                                                .processPayment(org.getEmail(), planName,
                                                                                plan.getPrice().doubleValue())
                                                                .flatMap(success -> {
                                                                        // --- LOGIQUE DE TEST : 2 MINUTES AU LIEU DE 30
                                                                        // JOURS ---
                                                                        // LocalDateTime testExpiry =
                                                                        // LocalDateTime.now().plusMinutes(2);
                                                                        org.setSubscriptionPlanId(plan.getId());
                                                                        // org.setSubscriptionExpiresAt(testExpiry);
                                                                        org.setSubscriptionExpiresAt(LocalDateTime.now()
                                                                                        .plusDays(plan.getDurationDays()));

                                                                        SubscriptionEntity subRecord = SubscriptionEntity
                                                                                        .builder()
                                                                                        .id(UUID.randomUUID())
                                                                                        .organizationId(organizationId)
                                                                                        .planType(plan.getName())
                                                                                        .status("ACTIVE")
                                                                                        .startDate(LocalDateTime.now())
                                                                                        .endDate(org.getSubscriptionExpiresAt())
                                                                                        .isNewRecord(true)
                                                                                        .build();

                                                                        return organizationRepository.save(org)
                                                                                        .then(subscriptionRepository
                                                                                                        .save(Objects.requireNonNull(
                                                                                                                        subRecord)))
                                                                                        .thenReturn(plan);
                                                                })));
        }

        @Transactional
        public Mono<OrganizationEntity> checkAndDowngrade(OrganizationEntity org) {
                if (org.getSubscriptionExpiresAt() != null
                                && org.getSubscriptionExpiresAt().isBefore(LocalDateTime.now())) {
                        return planRepository.findByName("FREE")
                                        .flatMap(freePlan -> {
                                                org.setSubscriptionPlanId(freePlan.getId());
                                                org.setSubscriptionExpiresAt(null);

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
                                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                                                .flatMap(plan -> {
                                                        // Optionnel : Empêcher la modification si c'est un plan FREE
                                                        if ("FREE".equalsIgnoreCase(plan.getName())) {
                                                                return Mono.error(new RuntimeException(
                                                                                "Le renouvellement automatique ne s'applique pas au plan FREE"));
                                                        }

                                                        org.setSubscriptionAutoRenew(autoRenew);
                                                        return organizationRepository.save(org);
                                                }))
                                .doOnSuccess(updated -> eventPublisher.publishEvent(new AuditEvent(
                                                "UPDATE_AUTO_RENEW",
                                                "SUBSCRIPTION",
                                                "Auto-renew set to " + autoRenew + " for " + updated.getName())));
        }
}
