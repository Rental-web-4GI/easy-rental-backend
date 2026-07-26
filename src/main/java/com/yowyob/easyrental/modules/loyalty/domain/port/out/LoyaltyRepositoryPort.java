package com.yowyob.easyrental.modules.loyalty.domain.port.out;

import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyLedgerEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Outgoing port for loyalty ledger persistence.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public interface LoyaltyRepositoryPort {

    Mono<LoyaltyLedgerEntity> save(LoyaltyLedgerEntity entity);

    /** Last balance_after for the client, or 0 if no ledger entries exist. */
    Mono<Integer> currentBalance(UUID clientId);

    Flux<LoyaltyLedgerEntity> history(UUID clientId);

    /** Sum of positive deltas over the last 12 months, or 0. */
    Mono<Integer> annualEarned(UUID clientId);
}
