package com.yowyob.easyrental.modules.loyalty.application;

import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyLedgerEntity;
import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyTier;
import com.yowyob.easyrental.modules.loyalty.domain.port.out.LoyaltyRepositoryPort;
import com.yowyob.easyrental.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyUseCaseImplTest {

    @Mock
    private LoyaltyRepositoryPort repo;

    @InjectMocks
    private LoyaltyUseCaseImpl useCase;

    @Test
    void earnFromRental_computesFloorDiv1000() {
        UUID clientId = UUID.randomUUID();
        UUID rentalId = UUID.randomUUID();

        when(repo.currentBalance(clientId)).thenReturn(Mono.just(0));
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.earnFromRental(clientId, new BigDecimal("105000"), rentalId))
                .expectNext(105)
                .verifyComplete();

        ArgumentCaptor<LoyaltyLedgerEntity> captor = ArgumentCaptor.forClass(LoyaltyLedgerEntity.class);
        verify(repo).save(captor.capture());
        LoyaltyLedgerEntity saved = captor.getValue();
        assertThat(saved.getDeltaPoints()).isEqualTo(105);
        assertThat(saved.getBalanceAfter()).isEqualTo(105);
        assertThat(saved.getSourceType()).isEqualTo("EARN_RENTAL");
        assertThat(saved.getClientId()).isEqualTo(clientId);
        assertThat(saved.getSourceId()).isEqualTo(rentalId);
    }

    @Test
    void earnFromRental_zeroPoints_noop() {
        UUID clientId = UUID.randomUUID();
        UUID rentalId = UUID.randomUUID();

        when(repo.currentBalance(clientId)).thenReturn(Mono.just(30));

        StepVerifier.create(useCase.earnFromRental(clientId, new BigDecimal("500"), rentalId))
                .expectNext(30)
                .verifyComplete();

        verify(repo, never()).save(any());
    }

    @Test
    void redeem_insufficient_throws() {
        UUID clientId = UUID.randomUUID();
        UUID rentalId = UUID.randomUUID();

        when(repo.currentBalance(clientId)).thenReturn(Mono.just(10));

        StepVerifier.create(useCase.redeem(clientId, 50, rentalId))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(ValidationException.class);
                    assertThat(err.getMessage()).isEqualTo("INSUFFICIENT_POINTS");
                })
                .verify();
    }

    @Test
    void redeem_ok_decrements() {
        UUID clientId = UUID.randomUUID();
        UUID rentalId = UUID.randomUUID();

        when(repo.currentBalance(clientId)).thenReturn(Mono.just(200));
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.redeem(clientId, 50, rentalId))
                .expectNext(150)
                .verifyComplete();

        ArgumentCaptor<LoyaltyLedgerEntity> captor = ArgumentCaptor.forClass(LoyaltyLedgerEntity.class);
        verify(repo).save(captor.capture());
        LoyaltyLedgerEntity saved = captor.getValue();
        assertThat(saved.getDeltaPoints()).isEqualTo(-50);
        assertThat(saved.getBalanceAfter()).isEqualTo(150);
        assertThat(saved.getSourceType()).isEqualTo("REDEEM_BOOKING");
    }

    @Test
    void tier_thresholds() {
        assertThat(LoyaltyTier.fromAnnualPoints(0)).isEqualTo(LoyaltyTier.BRONZE);
        assertThat(LoyaltyTier.fromAnnualPoints(500)).isEqualTo(LoyaltyTier.ARGENT);
        assertThat(LoyaltyTier.fromAnnualPoints(2000)).isEqualTo(LoyaltyTier.OR);
        assertThat(LoyaltyTier.fromAnnualPoints(5000)).isEqualTo(LoyaltyTier.PLATINE);
    }
}
