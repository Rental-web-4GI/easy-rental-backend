package com.yowyob.easyrental.modules.pricing.domain.port.out;

import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.shared.enums.ResourceType;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for pricing persistence.
 */
public interface PricingRepositoryPort {

    Mono<PricingEntity> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    Mono<PricingEntity> save(PricingEntity pricing);
}
