package com.yowyob.easyrental.modules.rating.domain.port.out;

import com.yowyob.easyrental.modules.rating.domain.RatingEntity;
import com.yowyob.easyrental.modules.rating.domain.TargetType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Outgoing port for rating persistence.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public interface RatingRepositoryPort {

    Mono<RatingEntity> save(RatingEntity e);

    Mono<RatingEntity> findByRentalIdAndRaterIdAndTargetType(UUID rentalId, UUID raterId, TargetType targetType);

    Flux<RatingEntity> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, UUID targetId);
}
