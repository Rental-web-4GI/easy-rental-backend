package com.yowyob.easyrental.modules.organization.domain.port.out;

import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for organization persistence.
 */
public interface OrganizationRepositoryPort {

    Mono<OrganizationEntity> findById(UUID id);

    Mono<OrganizationEntity> save(OrganizationEntity organization);

    Flux<OrganizationEntity> findAll();

    Flux<OrganizationEntity> findAllBySubscriptionPlanId(UUID subscriptionPlanId);

    Mono<OrganizationEntity> findByOwnerId(UUID ownerId);
}
