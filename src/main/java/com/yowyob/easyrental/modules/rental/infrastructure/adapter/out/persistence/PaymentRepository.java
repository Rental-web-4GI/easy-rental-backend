package com.yowyob.easyrental.modules.rental.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.rental.domain.PaymentEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

public interface PaymentRepository extends R2dbcRepository<PaymentEntity, UUID> {

    Flux<PaymentEntity> findAllByRentalId(UUID rentalId);

    // --- CLIENT : Paiements liés aux locations du client ---
    @Query("""
        SELECT p.*
        FROM payments p
        JOIN rentals r ON p.rental_id = r.id
        WHERE r.client_id = :clientId
        ORDER BY p.transaction_date DESC
    """)
    Flux<PaymentEntity> findAllByClientId(UUID clientId);

    // --- AGENCE : Paiements liés aux locations de l'agence ---
    @Query("""
        SELECT p.*
        FROM payments p
        JOIN rentals r ON p.rental_id = r.id
        WHERE r.agency_id = :agencyId
        ORDER BY p.transaction_date DESC
    """)
    Flux<PaymentEntity> findAllByAgencyId(UUID agencyId);

    // --- ORGANISATION : Paiements liés aux locations de toutes les agences ---
    @Query("""
        SELECT p.*
        FROM payments p
        JOIN rentals r ON p.rental_id = r.id
        JOIN agencies a ON r.agency_id = a.id
        WHERE a.organization_id = :orgId
        ORDER BY p.transaction_date DESC
    """)
    Flux<PaymentEntity> findAllRentalPaymentsByOrganizationId(UUID orgId);
}
