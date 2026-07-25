package com.yowyob.easyrental.modules.rating.application;

import com.yowyob.easyrental.modules.rating.domain.RaterType;
import com.yowyob.easyrental.modules.rating.domain.RatingEntity;
import com.yowyob.easyrental.modules.rating.domain.TargetType;
import com.yowyob.easyrental.modules.rating.domain.port.in.RatingUseCase;
import com.yowyob.easyrental.modules.rating.domain.port.out.RatingRepositoryPort;
import com.yowyob.easyrental.modules.rating.dto.RatingCreateRequest;
import com.yowyob.easyrental.modules.rating.dto.RatingResponseDTO;
import com.yowyob.easyrental.modules.rating.dto.RatingStatsDTO;
import com.yowyob.easyrental.modules.rating.mapper.RatingMapper;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import com.yowyob.easyrental.shared.exception.ResourceNotFoundException;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the post-rental rating use cases: one-shot submission guarded by
 * rental completion + uniqueness, and read-side listing/statistics per target.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Service
@RequiredArgsConstructor
public class RatingUseCaseImpl implements RatingUseCase {

    private final RatingRepositoryPort ratingRepository;
    private final RatingMapper mapper;
    private final RentalRepositoryPort rentalRepository;

    @Override
    public Mono<RatingResponseDTO> submitRating(RatingCreateRequest request) {
        if (request.stars() == null || request.stars() < 1 || request.stars() > 5) {
            return Mono.error(new ValidationException("STARS_OUT_OF_RANGE"));
        }

        return rentalRepository.findById(request.rentalId())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rental not found")))
                .flatMap(rental -> {
                    if (rental.getStatus() != RentalStatus.COMPLETED) {
                        return Mono.error(new ValidationException("RENTAL_NOT_COMPLETED"));
                    }

                    TargetType targetType = TargetType.valueOf(request.targetType());
                    RaterType raterType = RaterType.valueOf(request.raterType());

                    return ratingRepository.findByRentalIdAndRaterIdAndTargetType(
                                    request.rentalId(), request.raterId(), targetType)
                            .flatMap(existing -> Mono.<RatingEntity>error(new ValidationException("ALREADY_RATED")))
                            .switchIfEmpty(Mono.defer(() -> {
                                RatingEntity toSave = RatingEntity.builder()
                                        .id(UUID.randomUUID())
                                        .rentalId(request.rentalId())
                                        .raterType(raterType)
                                        .raterId(request.raterId())
                                        .targetType(targetType)
                                        .targetId(request.targetId())
                                        .stars(request.stars())
                                        .comment(request.comment())
                                        .createdAt(Instant.now())
                                        .isNewRecord(true)
                                        .build();
                                return ratingRepository.save(toSave);
                            }));
                })
                .map(mapper::toDto);
    }

    @Override
    public Flux<RatingResponseDTO> getRatingsForTarget(String targetType, UUID targetId, int page, int size) {
        TargetType type = TargetType.valueOf(targetType);
        return ratingRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(type, targetId)
                .skip((long) page * size)
                .take(size)
                .map(mapper::toDto);
    }

    @Override
    public Mono<RatingStatsDTO> getStatsForTarget(String targetType, UUID targetId) {
        TargetType type = TargetType.valueOf(targetType);
        return ratingRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(type, targetId)
                .collectList()
                .map(this::toStats);
    }

    private RatingStatsDTO toStats(List<RatingEntity> ratings) {
        long count = ratings.size();
        double average = ratings.stream()
                .mapToInt(RatingEntity::getStars)
                .average()
                .orElse(0.0);

        Map<Integer, Long> distribution = new HashMap<>();
        for (RatingEntity rating : ratings) {
            int star = rating.getStars();
            distribution.merge(star, 1L, Long::sum);
        }

        return new RatingStatsDTO(average, count, distribution);
    }
}
