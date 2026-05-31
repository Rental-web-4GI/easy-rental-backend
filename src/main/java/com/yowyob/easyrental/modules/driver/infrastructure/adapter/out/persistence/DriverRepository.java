package com.yowyob.easyrental.modules.driver.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.driver.domain.DriverEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface DriverRepository extends R2dbcRepository<DriverEntity, UUID> {
    Flux<DriverEntity> findAllByOrganizationId(UUID organizationId);
    Flux<DriverEntity> findAllByAgencyId(UUID agencyId);

    /**
     * Trouve les chauffeurs disponibles pour une agence sur une période donnée.
     * Un chauffeur est disponible si :
     * 1. Il appartient à l'agence.
     * 2. Son statut est ACTIVE.
     * 3. Il n'a AUCUNE entrée dans la table 'schedules' qui chevauche la période demandée.
     */
    @Query("""
        SELECT * FROM drivers d
        WHERE d.agency_id = :agencyId
        AND d.status = 'ACTIVE'
        AND d.id NOT IN (
            SELECT s.resource_id FROM schedules s
            WHERE s.resource_type = 'DRIVER'
            AND s.status IN ('RENTED', 'UNAVAILABLE', 'MAINTENANCE')
            AND (s.start_date < :endDate AND s.end_date > :startDate)
        )
    """)
    Flux<DriverEntity> findAvailableDrivers(UUID agencyId, LocalDateTime startDate, LocalDateTime endDate);
}
