package com.yowyob.easyrental.modules.pricing.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.shared.enums.ResourceType;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface PricingRepository extends R2dbcRepository<PricingEntity, UUID> {
    Mono<PricingEntity> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);
}
