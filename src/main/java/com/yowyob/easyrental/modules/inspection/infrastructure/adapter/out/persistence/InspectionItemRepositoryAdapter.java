package com.yowyob.easyrental.modules.inspection.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.inspection.domain.InspectionItemEntity;
import com.yowyob.easyrental.modules.inspection.domain.port.out.InspectionItemRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Adapter implementing {@link InspectionItemRepositoryPort} via R2DBC.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Component
@RequiredArgsConstructor
public class InspectionItemRepositoryAdapter implements InspectionItemRepositoryPort {

    private final InspectionItemRepository inspectionItemRepository;

    @Override
    public Flux<InspectionItemEntity> saveAll(Flux<InspectionItemEntity> items) {
        return inspectionItemRepository.saveAll(items);
    }

    @Override
    public Flux<InspectionItemEntity> findAllByInspectionId(UUID inspectionId) {
        return inspectionItemRepository.findAllByInspectionId(inspectionId);
    }
}
