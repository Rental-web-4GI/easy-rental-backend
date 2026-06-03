package com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubscriptionRepositoryAdapter implements SubscriptionRepositoryPort {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public Mono<SubscriptionEntity> findById(UUID id) {
        return subscriptionRepository.findById(id);
    }

    @Override
    public Mono<SubscriptionEntity> save(SubscriptionEntity subscription) {
        return subscriptionRepository.save(subscription);
    }

    @Override
    public Flux<SubscriptionEntity> findAllByOrganizationIdOrderByStartDateDesc(UUID organizationId) {
        return subscriptionRepository.findAllByOrganizationIdOrderByStartDateDesc(organizationId);
    }

    @Override
    public Mono<SubscriptionEntity> findFirstByOrganizationIdAndStatus(UUID organizationId, String status) {
        return subscriptionRepository.findFirstByOrganizationIdAndStatus(organizationId, status);
    }

    @Override
    public Flux<SubscriptionEntity> findAllByPlanType(String planType) {
        return subscriptionRepository.findAllByPlanType(planType);
    }
}
