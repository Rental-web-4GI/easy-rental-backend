package com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface VehicleRepository extends R2dbcRepository<VehicleEntity, UUID> {
    Flux<VehicleEntity> findAllByAgencyId(UUID agencyId);
    Flux<VehicleEntity> findAllByOrganizationId(UUID organizationId);

    // Récupère tous les véhicules par statut (ex: "AVAILABLE")
    Flux<VehicleEntity> findAllByStatut(String statut);

    @Query("SELECT organization_id FROM vehicles WHERE id = :vehicleId")
    Mono<UUID> findOrgIdByVehicleId(UUID vehicleId);

    Flux<VehicleEntity> findAllByOrganizationIdAndCategoryId(UUID organizationId, UUID categoryId);
    Flux<VehicleEntity> findAllByAgencyIdAndCategoryId(UUID agencyId, UUID categoryId);

    // Récupérer les véhicules d'une agence par statut
    Flux<VehicleEntity> findAllByAgencyIdAndStatut(UUID agencyId, String statut);

    //  Recherche avancée de véhicules disponibles
    @Query("SELECT * FROM vehicles WHERE statut = 'AVAILABLE' " +
           "AND (:agencyId::uuid IS NULL OR agency_id = :agencyId) " +
           "AND (:categoryId::uuid IS NULL OR category_id = :categoryId) " +
           "AND (:keyword::text IS NULL OR brand ILIKE '%' || :keyword || '%' OR model ILIKE '%' || :keyword || '%')")
    Flux<VehicleEntity> searchAvailableVehicles(UUID agencyId, UUID categoryId, String keyword);
}
