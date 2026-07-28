package com.yowyob.easyrental.modules.rating.domain.port.in;

import com.yowyob.easyrental.modules.rating.dto.RatingCreateRequest;
import com.yowyob.easyrental.modules.rating.dto.RatingResponseDTO;
import com.yowyob.easyrental.modules.rating.dto.RatingStatsDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Inbound port for the rating use cases: submission of post-rental ratings
 * and read access (list + stats) per target.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public interface RatingUseCase {

    Mono<RatingResponseDTO> submitRating(RatingCreateRequest request);

    Flux<RatingResponseDTO> getRatingsForTarget(String targetType, UUID targetId, int page, int size);

    Mono<RatingStatsDTO> getStatsForTarget(String targetType, UUID targetId);
}
