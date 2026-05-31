package com.yowyob.easyrental.modules.review.application;

import com.yowyob.easyrental.modules.driver.infrastructure.adapter.out.persistence.DriverRepository;
import com.yowyob.easyrental.modules.review.domain.ReviewEntity;
import com.yowyob.easyrental.modules.review.dto.ReviewRequestDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewResponseDTO;
import com.yowyob.easyrental.modules.review.infrastructure.adapter.out.persistence.ReviewRepository;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.VehicleRepository;
import com.yowyob.easyrental.shared.enums.ResourceType;
import com.yowyob.easyrental.modules.review.domain.port.in.ReviewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewUseCaseImpl implements ReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public Mono<ReviewResponseDTO> addReview(ReviewRequestDTO request) {
        ReviewEntity review = ReviewEntity.builder()
                .id(UUID.randomUUID())
                .resourceId(request.resourceId())
                .resourceType(request.resourceType())
                .rating(request.rating())
                .comment(request.comment())
                .authorName(request.authorName() != null ? request.authorName() : "Anonyme")
                .createdAt(LocalDateTime.now())
                .isNewRecord(true)
                .build();

        return reviewRepository.save(review)
                .flatMap(savedReview -> updateResourceRating(request.resourceType(), request.resourceId())
                        .thenReturn(mapToDto(savedReview)));
    }

    public Flux<ReviewResponseDTO> getReviews(ResourceType type, UUID resourceId) {
        return reviewRepository.findAllByResourceTypeAndResourceId(type, resourceId)
                .map(this::mapToDto);
    }

    // Met à jour la note moyenne dans la table parente (Vehicle ou Driver)
    private Mono<Void> updateResourceRating(ResourceType type, UUID resourceId) {
        return reviewRepository.getAverageRating(type, resourceId)
                .defaultIfEmpty(0.0)
                .flatMap(avg -> {
                    if (type == ResourceType.VEHICLE) {
                        return vehicleRepository.findById(resourceId)
                                .flatMap(v -> {
                                    v.setRating(avg);
                                    return vehicleRepository.save(v);
                                }).then();
                    } else if (type == ResourceType.DRIVER) {
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
                entity.getCreatedAt()
        );
    }
}
