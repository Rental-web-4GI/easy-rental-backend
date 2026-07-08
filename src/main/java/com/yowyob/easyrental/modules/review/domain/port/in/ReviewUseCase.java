package com.yowyob.easyrental.modules.review.domain.port.in;

import com.yowyob.easyrental.modules.review.dto.FeaturedReviewsResponseDTO;
import com.yowyob.easyrental.modules.review.dto.PlatformFeedbackRequestDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewModerationStatsDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewRequestDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewResponseDTO;
import com.yowyob.easyrental.shared.enums.ResourceType;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for review use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface ReviewUseCase {

    Mono<ReviewResponseDTO> addReview(ReviewRequestDTO request);

    Mono<ReviewResponseDTO> addPlatformFeedback(PlatformFeedbackRequestDTO request);

    Flux<ReviewResponseDTO> getReviews(ResourceType type, UUID resourceId);

    Mono<FeaturedReviewsResponseDTO> getFeaturedReviews(int limit);

    Flux<ReviewResponseDTO> listAllForAdmin();

    Mono<ReviewModerationStatsDTO> getModerationStats();

    Mono<ReviewResponseDTO> setPublished(UUID id, boolean published);
}
