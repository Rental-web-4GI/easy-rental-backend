package com.yowyob.easyrental.modules.loyalty.application;

import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyLedgerEntity;
import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyTier;
import com.yowyob.easyrental.modules.loyalty.domain.port.in.LoyaltyUseCase;
import com.yowyob.easyrental.modules.loyalty.domain.port.out.LoyaltyRepositoryPort;
import com.yowyob.easyrental.modules.loyalty.dto.LoyaltyBalanceDTO;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Implements the loyalty points use cases: earning points on completed
 * rentals (floor(amount/1000)), balance/tier computation and redemption
 * against the {@code loyalty_ledger} append-only ledger.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Service
@RequiredArgsConstructor
public class LoyaltyUseCaseImpl implements LoyaltyUseCase {

    private static final String SOURCE_EARN_RENTAL = "EARN_RENTAL";
    private static final String SOURCE_REDEEM_BOOKING = "REDEEM_BOOKING";

    private final LoyaltyRepositoryPort repo;

    @Override
    public Mono<Integer> earnFromRental(UUID clientId, BigDecimal rentalPortionTotal, UUID rentalId) {
        if (clientId == null) {
            return Mono.just(0);
        }

        int pts = rentalPortionTotal == null
                ? 0
                : rentalPortionTotal.divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR).intValue();

        if (pts <= 0) {
            return repo.currentBalance(clientId);
        }

        return repo.currentBalance(clientId)
                .flatMap(bal -> {
                    int newBal = bal + pts;
                    return repo.save(entity(clientId, pts, SOURCE_EARN_RENTAL, rentalId, newBal))
                            .thenReturn(newBal);
                });
    }

    @Override
    public Mono<LoyaltyBalanceDTO> getBalance(UUID clientId) {
        return Mono.zip(repo.currentBalance(clientId), repo.annualEarned(clientId))
                .map(t -> new LoyaltyBalanceDTO(
                        t.getT1(),
                        LoyaltyTier.fromAnnualPoints(t.getT2()).name(),
                        t.getT2()));
    }

    @Override
    public Mono<Integer> redeem(UUID clientId, int points, UUID rentalId) {
        if (points <= 0) {
            return Mono.error(new ValidationException("INVALID_POINTS"));
        }

        return repo.currentBalance(clientId)
                .flatMap(bal -> {
                    if (bal < points) {
                        return Mono.error(new ValidationException("INSUFFICIENT_POINTS"));
                    }
                    int newBal = bal - points;
                    return repo.save(entity(clientId, -points, SOURCE_REDEEM_BOOKING, rentalId, newBal))
                            .thenReturn(newBal);
                });
    }

    @Override
    public Flux<LoyaltyLedgerEntity> history(UUID clientId) {
        return repo.history(clientId);
    }

    private LoyaltyLedgerEntity entity(UUID clientId, int deltaPoints, String sourceType, UUID sourceId,
            int balanceAfter) {
        return LoyaltyLedgerEntity.builder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .deltaPoints(deltaPoints)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .balanceAfter(balanceAfter)
                .createdAt(Instant.now())
                .isNewRecord(true)
                .build();
    }
}
