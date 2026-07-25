package com.yowyob.easyrental.modules.inspection.domain.port.out;

import com.yowyob.easyrental.modules.inspection.domain.RentalInspectionEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for persisting {@link RentalInspectionEntity}.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public interface InspectionRepositoryPort {
    Mono<RentalInspectionEntity> save(RentalInspectionEntity entity);
    Mono<RentalInspectionEntity> findById(UUID id);
    Flux<RentalInspectionEntity> findAllByRentalId(UUID rentalId);
}
