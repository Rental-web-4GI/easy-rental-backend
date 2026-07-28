package com.yowyob.easyrental.modules.inspection.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.inspection.domain.RentalInspectionEntity;
import com.yowyob.easyrental.modules.inspection.domain.port.out.InspectionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter implementing {@link InspectionRepositoryPort} via R2DBC.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Component
@RequiredArgsConstructor
public class InspectionRepositoryAdapter implements InspectionRepositoryPort {

    private final InspectionRepository inspectionRepository;

    @Override
    public Mono<RentalInspectionEntity> save(RentalInspectionEntity entity) {
        return inspectionRepository.save(entity);
    }

    @Override
    public Mono<RentalInspectionEntity> findById(UUID id) {
        return inspectionRepository.findById(id);
    }

    @Override
    public Flux<RentalInspectionEntity> findAllByRentalId(UUID rentalId) {
        return inspectionRepository.findAllByRentalId(rentalId);
    }
}
