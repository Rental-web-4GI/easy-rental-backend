package com.yowyob.easyrental.modules.review.application;

import com.yowyob.easyrental.modules.driver.domain.port.out.DriverRepositoryPort;
import com.yowyob.easyrental.modules.review.domain.ReviewConstants;
import com.yowyob.easyrental.modules.review.domain.ReviewEntity;
import com.yowyob.easyrental.modules.review.domain.port.in.ReviewUseCase;
import com.yowyob.easyrental.modules.review.domain.port.out.ReviewRepositoryPort;
import com.yowyob.easyrental.modules.review.dto.FeaturedReviewsResponseDTO;
import com.yowyob.easyrental.modules.review.dto.PlatformFeedbackRequestDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewModerationStatsDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewRequestDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewResponseDTO;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import com.yowyob.easyrental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Review use cases including landing feedback and admin moderation.
 */
@Service
@RequiredArgsConstructor
public class ReviewUseCaseImpl implements ReviewUseCase {

    private final ReviewRepositoryPort reviewRepository;
    private final VehicleRepositoryPort vehicleRepository;
    private final DriverRepositoryPort driverRepository;

    @Override
    @Transactional
    public Mono<ReviewResponseDTO> addReview(ReviewRequestDTO request) {
        ReviewEntity review = ReviewEntity.builder()
                .id(UUID.randomUUID())
                .resourceId(request.resourceId())
                .resourceType(request.resourceType())
                .rating(request.rating())
                .comment(request.comment())
                .authorName(request.authorName() != null ? request.authorName() : "Anonyme")
                .published(Boolean.FALSE)
                .createdAt(LocalDateTime.now())
                .isNewRecord(true)
                .build();

        return reviewRepository.save(review)
                .flatMap(savedReview -> updateResourceRating(request.resourceType(), request.resourceId())
                        .thenReturn(mapToDto(savedReview)));
    }

    @Override
    @Transactional
    public Mono<ReviewResponseDTO> addPlatformFeedback(PlatformFeedbackRequestDTO request) {
        ReviewEntity review = ReviewEntity.builder()
                .id(UUID.randomUUID())
                .resourceId(ReviewConstants.PLATFORM_RESOURCE_ID)
                .resourceType(ResourceType.PLATFORM)
                .rating(request.rating())
                .comment(request.comment())
                .authorName(request.authorName())
                .authorRole(request.authorRole())
                .published(Boolean.FALSE)
                .createdAt(LocalDateTime.now())
                .isNewRecord(true)
                .build();

        return reviewRepository.save(review).map(this::mapToDto);
    }

    @Override
    public Flux<ReviewResponseDTO> getReviews(ResourceType type, UUID resourceId) {
        return reviewRepository.findAllByResourceTypeAndResourceId(type, resourceId)
                .map(this::mapToDto);
    }

    @Override
    public Mono<FeaturedReviewsResponseDTO> getFeaturedReviews(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 12);
        return Mono.zip(
                reviewRepository.findPublishedLatest(safeLimit).map(this::mapToDto).collectList(),
                reviewRepository.getPublishedAverageRating().defaultIfEmpty(0.0),
                reviewRepository.countPublished().defaultIfEmpty(0L)
        ).map(tuple -> new FeaturedReviewsResponseDTO(tuple.getT1(), tuple.getT2(), tuple.getT3()));
    }

    @Override
    public Flux<ReviewResponseDTO> listAllForAdmin() {
        return reviewRepository.findAllOrderedByCreatedAtDesc().map(this::mapToDto);
    }

    @Override
    public Mono<ReviewModerationStatsDTO> getModerationStats() {
        return Mono.zip(
                reviewRepository.countPublished().defaultIfEmpty(0L),
                reviewRepository.countUnpublished().defaultIfEmpty(0L)
        ).map(tuple -> new ReviewModerationStatsDTO(tuple.getT1(), tuple.getT2()));
    }

    @Override
    @Transactional
    public Mono<ReviewResponseDTO> setPublished(UUID id, boolean published) {
        return reviewRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Review not found: " + id)))
                .flatMap(review -> {
                    review.setPublished(published);
                    return reviewRepository.save(review);
                })
                .map(this::mapToDto);
    }

    private Mono<Void> updateResourceRating(ResourceType type, UUID resourceId) {
        if (type == ResourceType.PLATFORM) {
            return Mono.empty();
        }

        return reviewRepository.getAverageRating(type, resourceId)
                .defaultIfEmpty(0.0)
                .flatMap(avg -> {
                    if (type == ResourceType.VEHICLE) {
                        return vehicleRepository.findById(resourceId)
                                .flatMap(v -> {
                                    v.setRating(avg);
                                    return vehicleRepository.save(v);
                                }).then();
                    }
                    if (type == ResourceType.DRIVER) {
                        return driverRepository.findById(resourceId)
                                .flatMap(d -> {
                                    d.setRating(avg);
                                    return driverRepository.save(d);
                                }).then();
                    }
                    return Mono.empty();
                });
    }

    private ReviewResponseDTO mapToDto(ReviewEntity entity) {
        return new ReviewResponseDTO(
                entity.getId(),
                entity.getResourceId(),
                entity.getResourceType(),
                entity.getRating(),
                entity.getComment(),
                entity.getAuthorName(),
                entity.getAuthorRole(),
                Boolean.TRUE.equals(entity.getPublished()),
                resolveSourceLabel(entity.getResourceType()),
                entity.getCreatedAt()
        );
    }

    private String resolveSourceLabel(ResourceType type) {
        if (type == ResourceType.PLATFORM) {
            return "Landing";
        }
        if (type == ResourceType.VEHICLE) {
            return "Véhicule";
        }
        if (type == ResourceType.DRIVER) {
            return "Chauffeur";
        }
        return type.name();
    }
}
