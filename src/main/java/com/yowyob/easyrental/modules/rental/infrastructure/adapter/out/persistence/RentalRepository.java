package com.yowyob.easyrental.modules.rental.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RentalRepository extends R2dbcRepository<RentalEntity, UUID> {

    Flux<RentalEntity> findAllByAgencyId(UUID agencyId);
    Flux<RentalEntity> findAllByClientId(UUID clientId);

    // Filtres par statuts
    Flux<RentalEntity> findAllByClientIdAndStatusIn(UUID clientId, List<RentalStatus> statuses);
    Flux<RentalEntity> findAllByAgencyIdAndStatusIn(UUID agencyId, List<RentalStatus> statuses);

    @Query("""
        SELECT r.*
        FROM rentals r
        JOIN agencies a ON r.agency_id = a.id
        WHERE a.organization_id = :orgId
        AND r.status IN (:statuses)
        ORDER BY r.created_at DESC
    """)
    Flux<RentalEntity> findAllByOrganizationIdAndStatusIn(UUID orgId, List<RentalStatus> statuses);

    /** Dossiers du client, dans les agences d'une organisation, avec dette (supplément) impayée. */
    @Query("""
        SELECT r.*
        FROM rentals r
        JOIN agencies a ON r.agency_id = a.id
        WHERE a.organization_id = :orgId
        AND r.client_id = :clientId
        AND COALESCE(r.supplement_due, 0) > 0
        ORDER BY r.updated_at ASC
    """)
    Flux<RentalEntity> findClientDebtRentals(UUID clientId, UUID orgId);

    // NOUVEAU : Chercher une réservation PENDING existante pour éviter les doublons
    @Query("SELECT * FROM rentals WHERE client_id = :clientId AND vehicle_id = :vehicleId AND status = 'PENDING' LIMIT"
            + " 1")
    Mono<RentalEntity> findExistingPendingRental(UUID clientId, UUID vehicleId);

    @Query("SELECT COUNT(*) FROM rentals WHERE vehicle_id = :vehicleId AND start_date < :checkEnd AND end_date >"
            + " :checkStart AND status NOT IN ('CANCELLED', 'COMPLETED')")
    Mono<Long> countConflictingRentals(UUID vehicleId, LocalDateTime checkStart, LocalDateTime checkEnd);

    Mono<Long> countByStatus(RentalStatus status);

    @Query("SELECT COUNT(*) FROM rentals WHERE status = 'COMPLETED' "
            + "AND EXTRACT(MONTH FROM updated_at) = EXTRACT(MONTH FROM CURRENT_DATE) "
            + "AND EXTRACT(YEAR FROM updated_at) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Mono<Long> countCompletedThisMonth();
}
