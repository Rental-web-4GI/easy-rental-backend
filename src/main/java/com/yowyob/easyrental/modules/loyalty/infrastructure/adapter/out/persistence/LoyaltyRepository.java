package com.yowyob.easyrental.modules.loyalty.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyLedgerEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Spring Data R2DBC repository for {@link LoyaltyLedgerEntity}.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Repository
public interface LoyaltyRepository extends R2dbcRepository<LoyaltyLedgerEntity, UUID> {

    Mono<LoyaltyLedgerEntity> findFirstByClientIdOrderByCreatedAtDesc(UUID clientId);

    Flux<LoyaltyLedgerEntity> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    @Query("SELECT COALESCE(SUM(delta_points),0) FROM loyalty_ledger WHERE client_id = :clientId "
            + "AND delta_points > 0 AND created_at > now() - interval '12 months'")
    Mono<Integer> annualEarned(UUID clientId);
}
