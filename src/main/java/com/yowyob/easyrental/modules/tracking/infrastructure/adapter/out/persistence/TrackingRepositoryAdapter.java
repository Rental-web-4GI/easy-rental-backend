package com.yowyob.easyrental.modules.tracking.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.tracking.domain.RentalPositionEntity;
import com.yowyob.easyrental.modules.tracking.domain.port.out.TrackingRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter implementing {@link TrackingRepositoryPort} via R2DBC.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Component
@RequiredArgsConstructor
public class TrackingRepositoryAdapter implements TrackingRepositoryPort {

    private final RentalPositionRepository repo;

    @Override
    public Mono<RentalPositionEntity> save(RentalPositionEntity entity) {
        return repo.save(entity);
    }

    @Override
    public Flux<RentalPositionEntity> findAllByRentalIdOrderByRecordedAtAsc(UUID rentalId) {
        return repo.findAllByRentalIdOrderByRecordedAtAsc(rentalId);
    }
}
