package com.yowyob.easyrental.modules.vehicle.domain.port.out;

import com.yowyob.easyrental.modules.vehicle.domain.VehicleCategoryEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for category persistence.
 */
public interface CategoryRepositoryPort {

    Mono<VehicleCategoryEntity> findById(UUID id);

    Mono<VehicleCategoryEntity> save(VehicleCategoryEntity category);

    Flux<VehicleCategoryEntity> findAll();

    Flux<VehicleCategoryEntity> findAllByOrganizationIdOrSystem(UUID orgId);

    Flux<VehicleCategoryEntity> findAllByAgencyIdOrSystem(UUID agencyId);

    Mono<UUID> findOrgIdByCategoryId(UUID categoryId);

    Mono<Void> deleteById(UUID id);
}
