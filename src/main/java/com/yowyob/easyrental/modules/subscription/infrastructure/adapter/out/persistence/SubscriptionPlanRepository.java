package com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface SubscriptionPlanRepository extends R2dbcRepository<SubscriptionPlanEntity, UUID> {
    Mono<SubscriptionPlanEntity> findByName(String name);
}