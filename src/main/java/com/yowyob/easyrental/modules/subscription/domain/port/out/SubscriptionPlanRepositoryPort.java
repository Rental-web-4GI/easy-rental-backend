package com.yowyob.easyrental.modules.subscription.domain.port.out;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for subscription plan persistence.
 */
public interface SubscriptionPlanRepositoryPort {

    Mono<SubscriptionPlanEntity> findByName(String name);

    Mono<SubscriptionPlanEntity> findById(UUID id);

    Flux<SubscriptionPlanEntity> findAll();

    Mono<SubscriptionPlanEntity> save(SubscriptionPlanEntity plan);
}
