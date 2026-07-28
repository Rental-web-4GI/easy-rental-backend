package com.yowyob.easyrental.modules.tracking.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.tracking.domain.RentalPositionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface RentalPositionRepository extends R2dbcRepository<RentalPositionEntity, UUID> {
    Flux<RentalPositionEntity> findAllByRentalIdOrderByRecordedAtAsc(UUID rentalId);
}
