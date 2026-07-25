package com.yowyob.easyrental.modules.rating.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.rating.domain.RatingEntity;
import com.yowyob.easyrental.modules.rating.domain.TargetType;
import com.yowyob.easyrental.modules.rating.domain.port.out.RatingRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter implementing {@link RatingRepositoryPort} via R2DBC.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Component
@RequiredArgsConstructor
public class RatingRepositoryAdapter implements RatingRepositoryPort {

    private final RatingRepository ratingRepository;

    @Override
    public Mono<RatingEntity> save(RatingEntity e) {
        return ratingRepository.save(e);
    }

    @Override
    public Mono<RatingEntity> findByRentalIdAndRaterIdAndTargetType(
            UUID rentalId, UUID raterId, TargetType targetType) {
        return ratingRepository.findByRentalIdAndRaterIdAndTargetType(rentalId, raterId, targetType);
    }

    @Override
    public Flux<RatingEntity> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, UUID targetId) {
        return ratingRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
    }
}
