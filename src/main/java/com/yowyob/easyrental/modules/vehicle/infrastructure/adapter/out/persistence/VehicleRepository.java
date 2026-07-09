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

    @Query("SELECT COUNT(*) FROM vehicles WHERE category_id = :categoryId")
    Mono<Long> countByCategoryId(UUID categoryId);

    @Query("SELECT COUNT(*) FROM vehicles WHERE organization_id = :organizationId")
    Mono<Long> countByOrganizationId(UUID organizationId);

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

    /** Catalogue client : véhicules réservables (abonnement valide, agence en ligne, tarif défini). */
    @Query("""
            SELECT v.* FROM vehicles v
            INNER JOIN agencies a ON a.id = v.agency_id
            INNER JOIN organizations o ON o.id = v.organization_id
            WHERE v.statut = 'AVAILABLE'
              AND COALESCE(a.allow_online_booking, true) = true
              AND (o.subscription_expires_at IS NULL OR o.subscription_expires_at > NOW())
              AND LENGTH(TRIM(COALESCE(v.brand, ''))) >= 3
              AND LENGTH(TRIM(COALESCE(v.model, ''))) >= 2
              AND EXISTS (
                SELECT 1 FROM pricings p
                WHERE p.resource_id = v.id
                  AND p.resource_type = 'VEHICLE'
                  AND (COALESCE(p.price_per_day, 0) > 0 OR COALESCE(p.price_per_hour, 0) > 0)
              )
            """)
    Flux<VehicleEntity> findCatalogAvailableVehicles();
}
