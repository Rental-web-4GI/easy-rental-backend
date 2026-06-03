package com.yowyob.easyrental.modules.pricing.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.pricing.domain.port.out.PricingRepositoryPort;
import com.yowyob.easyrental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PricingRepositoryAdapter implements PricingRepositoryPort {

    private final PricingRepository pricingRepository;

    @Override
    public Mono<PricingEntity> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId) {
        return pricingRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    public Mono<PricingEntity> save(PricingEntity pricing) {
        return pricingRepository.save(pricing);
    }
}
