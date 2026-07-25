package com.yowyob.easyrental.modules.inspection.domain.port.out;

import com.yowyob.easyrental.modules.inspection.domain.InspectionItemEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;

/**
 * Outgoing port for persisting {@link InspectionItemEntity}.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public interface InspectionItemRepositoryPort {
    Flux<InspectionItemEntity> saveAll(Flux<InspectionItemEntity> items);
    Flux<InspectionItemEntity> findAllByInspectionId(UUID inspectionId);
}
