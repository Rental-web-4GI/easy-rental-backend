package com.yowyob.easyrental.modules.inspection.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.inspection.domain.InspectionItemEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface InspectionItemRepository extends R2dbcRepository<InspectionItemEntity, UUID> {
    Flux<InspectionItemEntity> findAllByInspectionId(UUID inspectionId);
}
