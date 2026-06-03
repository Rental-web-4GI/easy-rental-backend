package com.yowyob.easyrental.modules.vehicle.domain.port.out;

import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for vehicle persistence.
 */
public interface VehicleRepositoryPort {

    Mono<VehicleEntity> findById(UUID id);

    Mono<VehicleEntity> save(VehicleEntity vehicle);

    Mono<Void> delete(VehicleEntity vehicle);

    Flux<VehicleEntity> findAllByAgencyId(UUID agencyId);

    Flux<VehicleEntity> findAllByOrganizationId(UUID organizationId);

    Flux<VehicleEntity> findAllByStatut(String statut);

    Mono<UUID> findOrgIdByVehicleId(UUID vehicleId);

    Flux<VehicleEntity> findAllByOrganizationIdAndCategoryId(UUID organizationId, UUID categoryId);

    Flux<VehicleEntity> findAllByAgencyIdAndCategoryId(UUID agencyId, UUID categoryId);

    Flux<VehicleEntity> findAllByAgencyIdAndStatut(UUID agencyId, String statut);

    Flux<VehicleEntity> searchAvailableVehicles(UUID agencyId, UUID categoryId, String keyword);
}
