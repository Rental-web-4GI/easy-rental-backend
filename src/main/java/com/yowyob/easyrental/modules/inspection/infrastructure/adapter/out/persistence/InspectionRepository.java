package com.yowyob.easyrental.modules.inspection.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.inspection.domain.RentalInspectionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface InspectionRepository extends R2dbcRepository<RentalInspectionEntity, UUID> {
    Flux<RentalInspectionEntity> findAllByRentalId(UUID rentalId);
}
