package com.yowyob.easyrental.modules.subscription.domain.port.in;

import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionRemainingTimeDTO;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for subscription use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface SubscriptionUseCase {
    Mono<Void> initializeDefaultSubscription(UUID organizationId);
    Mono<SubscriptionPlanEntity> upgradePlan(UUID organizationId, String planName);
    Mono<OrganizationEntity> checkAndDowngrade(OrganizationEntity org);
    Mono<SubscriptionRemainingTimeDTO> getRemainingTime(UUID orgId);
    Mono<SubscriptionEntity> createHistoryRecord(UUID organizationId, String planName,
                        LocalDateTime endDate);
    Mono<OrganizationEntity> toggleAutoRenew(UUID orgId, boolean autoRenew);
    Flux<SubscriptionPlanEntity> getAllPlans();
    Mono<SubscriptionPlanEntity> updatePlan(UUID id, SubscriptionPlanEntity planUpdate);
    Mono<SubscriptionResponseDTO> getOrgSubscriptionStatus(UUID orgId);
    Mono<SubscriptionResponseDTO> buildSubscriptionResponse(OrganizationEntity org);
}
