package com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionPlanRepositoryAdapter implements SubscriptionPlanRepositoryPort {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
    public Mono<SubscriptionPlanEntity> findByName(String name) {
        return subscriptionPlanRepository.findByName(name);
    }

    @Override
    public Mono<SubscriptionPlanEntity> findById(UUID id) {
        return subscriptionPlanRepository.findById(id);
    }

    @Override
    public Flux<SubscriptionPlanEntity> findAll() {
        return subscriptionPlanRepository.findAll();
    }

    @Override
    public Mono<SubscriptionPlanEntity> save(SubscriptionPlanEntity plan) {
        return subscriptionPlanRepository.save(plan);
    }
}
