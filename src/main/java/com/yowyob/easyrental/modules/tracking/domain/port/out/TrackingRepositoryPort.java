package com.yowyob.easyrental.modules.tracking.domain.port.out;

import com.yowyob.easyrental.modules.tracking.domain.RentalPositionEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Outbound persistence port for rental GPS positions.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public interface TrackingRepositoryPort {

    Mono<RentalPositionEntity> save(RentalPositionEntity entity);

    Flux<RentalPositionEntity> findAllByRentalIdOrderByRecordedAtAsc(UUID rentalId);
}
