package com.yowyob.easyrental.modules.loyalty.domain.port.in;

import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyLedgerEntity;
import com.yowyob.easyrental.modules.loyalty.dto.LoyaltyBalanceDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound port for the loyalty points use cases: earning on completed
 * rentals, balance/tier read access, and redemption.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public interface LoyaltyUseCase {

    Mono<Integer> earnFromRental(UUID clientId, BigDecimal rentalPortionTotal, UUID rentalId);

    Mono<LoyaltyBalanceDTO> getBalance(UUID clientId);

    Mono<Integer> redeem(UUID clientId, int points, UUID rentalId);

    Flux<LoyaltyLedgerEntity> history(UUID clientId);
}
