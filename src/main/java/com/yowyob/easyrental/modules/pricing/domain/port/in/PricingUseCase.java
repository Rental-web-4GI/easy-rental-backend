package com.yowyob.easyrental.modules.pricing.domain.port.in;

import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.shared.enums.ResourceType;
import java.math.BigDecimal;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Incoming port for pricing use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface PricingUseCase {
    Mono<PricingEntity> setPricing(
            UUID orgId,
            ResourceType type,
            UUID resourceId,
            BigDecimal perHour,
            BigDecimal perDay);
    Mono<PricingEntity> getPricing(ResourceType type, UUID resourceId);
}
