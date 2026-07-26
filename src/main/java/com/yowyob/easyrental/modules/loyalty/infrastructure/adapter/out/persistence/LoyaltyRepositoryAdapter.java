package com.yowyob.easyrental.modules.loyalty.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyLedgerEntity;
import com.yowyob.easyrental.modules.loyalty.domain.port.out.LoyaltyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter implementing {@link LoyaltyRepositoryPort} via R2DBC.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Component
@RequiredArgsConstructor
public class LoyaltyRepositoryAdapter implements LoyaltyRepositoryPort {

    private final LoyaltyRepository loyaltyRepository;

    @Override
    public Mono<LoyaltyLedgerEntity> save(LoyaltyLedgerEntity entity) {
        return loyaltyRepository.save(entity);
    }

    @Override
    public Mono<Integer> currentBalance(UUID clientId) {
        return loyaltyRepository.findFirstByClientIdOrderByCreatedAtDesc(clientId)
                .map(LoyaltyLedgerEntity::getBalanceAfter)
                .defaultIfEmpty(0);
    }

    @Override
    public Flux<LoyaltyLedgerEntity> history(UUID clientId) {
        return loyaltyRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Override
    public Mono<Integer> annualEarned(UUID clientId) {
        return loyaltyRepository.annualEarned(clientId);
    }
}
