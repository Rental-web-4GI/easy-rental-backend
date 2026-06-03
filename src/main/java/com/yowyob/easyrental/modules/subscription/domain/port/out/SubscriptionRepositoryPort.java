package com.yowyob.easyrental.modules.subscription.domain.port.out;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for subscription persistence.
 */
public interface SubscriptionRepositoryPort {

    Mono<SubscriptionEntity> findById(UUID id);

    Mono<SubscriptionEntity> save(SubscriptionEntity subscription);

    Flux<SubscriptionEntity> findAllByOrganizationIdOrderByStartDateDesc(UUID organizationId);

    Mono<SubscriptionEntity> findFirstByOrganizationIdAndStatus(UUID organizationId, String status);

    Flux<SubscriptionEntity> findAllByPlanType(String planType);
}
