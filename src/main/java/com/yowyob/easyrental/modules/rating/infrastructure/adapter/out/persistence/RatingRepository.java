package com.yowyob.easyrental.modules.rating.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.rating.domain.RatingEntity;
import com.yowyob.easyrental.modules.rating.domain.TargetType;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Spring Data R2DBC repository for {@link RatingEntity}.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Repository
public interface RatingRepository extends R2dbcRepository<RatingEntity, UUID> {

    Mono<RatingEntity> findByRentalIdAndRaterIdAndTargetType(UUID rentalId, UUID raterId, TargetType targetType);

    Flux<RatingEntity> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, UUID targetId);
}
