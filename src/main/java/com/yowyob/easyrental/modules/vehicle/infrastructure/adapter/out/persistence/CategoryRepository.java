package com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.vehicle.domain.VehicleCategoryEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface CategoryRepository extends R2dbcRepository<VehicleCategoryEntity, UUID> {

    // Récupère les catégories via l'ID de l'organisation + Système
    @Query("SELECT * FROM vehicle_categories WHERE organization_id = :orgId OR organization_id IS NULL ORDER BY name"
            + " ASC")
    Flux<VehicleCategoryEntity> findAllByOrganizationIdOrSystem(UUID orgId);

    /**
     * Récupère toutes les catégories utilisables par une agence.
     * La sous-requête récupère l'organisation_id de l'agence fournie.
     */
    @Query("SELECT * FROM vehicle_categories " +
           "WHERE organization_id = (SELECT organization_id FROM agencies WHERE id = :agencyId) " +
           "OR organization_id IS NULL ORDER BY name ASC")
    Flux<VehicleCategoryEntity> findAllByAgencyIdOrSystem(UUID agencyId);
    // Dans CategoryRepository.java

    @Query("SELECT organization_id FROM vehicle_categories WHERE id = :categoryId")
    Mono<UUID> findOrgIdByCategoryId(UUID categoryId);
}
