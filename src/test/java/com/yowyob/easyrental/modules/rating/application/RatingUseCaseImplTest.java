package com.yowyob.easyrental.modules.rating.application;

import com.yowyob.easyrental.modules.rating.domain.RaterType;
import com.yowyob.easyrental.modules.rating.domain.RatingEntity;
import com.yowyob.easyrental.modules.rating.domain.TargetType;
import com.yowyob.easyrental.modules.rating.domain.port.out.RatingRepositoryPort;
import com.yowyob.easyrental.modules.rating.dto.RatingCreateRequest;
import com.yowyob.easyrental.modules.rating.dto.RatingResponseDTO;
import com.yowyob.easyrental.modules.rating.dto.RatingStatsDTO;
import com.yowyob.easyrental.modules.rating.mapper.RatingMapper;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import com.yowyob.easyrental.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RatingUseCaseImplTest {

    @Mock
    private RatingRepositoryPort ratingRepository;

    @Mock
    private RatingMapper mapper;

    @Mock
    private RentalRepositoryPort rentalRepository;

    @InjectMocks
    private RatingUseCaseImpl ratingUseCase;

    @Test
    void submitRating_rentalNotCompleted_throws() {
        UUID rentalId = UUID.randomUUID();
        UUID raterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        RentalEntity rental = RentalEntity.builder()
                .id(rentalId)
                .status(RentalStatus.ONGOING)
                .build();

        RatingCreateRequest request = new RatingCreateRequest(
                rentalId, "CLIENT", raterId, "AGENCY", targetId, (short) 5, "great");

        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));

        StepVerifier.create(ratingUseCase.submitRating(request))
                .expectErrorMatches(err -> err instanceof ValidationException
                        && err.getMessage().equals("RENTAL_NOT_COMPLETED"))
                .verify();
    }

    @Test
    void submitRating_alreadyRated_throws() {
        UUID rentalId = UUID.randomUUID();
        UUID raterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        RentalEntity rental = RentalEntity.builder()
                .id(rentalId)
                .status(RentalStatus.COMPLETED)
                .build();

        RatingCreateRequest request = new RatingCreateRequest(
                rentalId, "CLIENT", raterId, "AGENCY", targetId, (short) 5, "great");

        RatingEntity existing = RatingEntity.builder()
                .id(UUID.randomUUID())
                .rentalId(rentalId)
                .raterId(raterId)
                .targetType(TargetType.AGENCY)
                .build();

        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(ratingRepository.findByRentalIdAndRaterIdAndTargetType(rentalId, raterId, TargetType.AGENCY))
                .thenReturn(Mono.just(existing));

        StepVerifier.create(ratingUseCase.submitRating(request))
                .expectErrorMatches(err -> err instanceof ValidationException
                        && err.getMessage().equals("ALREADY_RATED"))
                .verify();
    }

    @Test
    void submitRating_valid_savesAndReturnsDto() {
        UUID rentalId = UUID.randomUUID();
        UUID raterId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        RentalEntity rental = RentalEntity.builder()
                .id(rentalId)
                .status(RentalStatus.COMPLETED)
                .build();

        RatingCreateRequest request = new RatingCreateRequest(
                rentalId, "CLIENT", raterId, "AGENCY", targetId, (short) 5, "great");

        when(rentalRepository.findById(rentalId)).thenReturn(Mono.just(rental));
        when(ratingRepository.findByRentalIdAndRaterIdAndTargetType(rentalId, raterId, TargetType.AGENCY))
                .thenReturn(Mono.empty());
        when(ratingRepository.save(any(RatingEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        RatingResponseDTO expectedDto = new RatingResponseDTO(
                UUID.randomUUID(), rentalId, "CLIENT", raterId, "AGENCY", targetId, (short) 5, "great", Instant.now());
        when(mapper.toDto(any(RatingEntity.class))).thenReturn(expectedDto);

        StepVerifier.create(ratingUseCase.submitRating(request))
                .expectNextMatches(dto -> dto.stars() == 5)
                .verifyComplete();

        verify(ratingRepository).save(any(RatingEntity.class));
    }

    @Test
    void getStatsForTarget_computesAverageAndDistribution() {
        UUID targetId = UUID.randomUUID();

        RatingEntity r1 = RatingEntity.builder().id(UUID.randomUUID()).stars((short) 5).build();
        RatingEntity r2 = RatingEntity.builder().id(UUID.randomUUID()).stars((short) 4).build();
        RatingEntity r3 = RatingEntity.builder().id(UUID.randomUUID()).stars((short) 5).build();

        when(ratingRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType.AGENCY, targetId))
                .thenReturn(Flux.just(r1, r2, r3));

        StepVerifier.create(ratingUseCase.getStatsForTarget("AGENCY", targetId))
                .expectNextMatches(stats -> stats.count() == 3
                        && Math.abs(stats.average() - 4.6667) < 0.01
                        && stats.distribution().get(5) == 2L
                        && stats.distribution().get(4) == 1L)
                .verifyComplete();
    }
}
