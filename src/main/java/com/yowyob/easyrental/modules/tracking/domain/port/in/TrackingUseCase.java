package com.yowyob.easyrental.modules.tracking.domain.port.in;

import com.yowyob.easyrental.modules.tracking.dto.PositionRequest;
import com.yowyob.easyrental.modules.tracking.dto.PositionResponseDTO;
import com.yowyob.easyrental.modules.tracking.dto.TrackingSummaryDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Inbound use case for GPS tracking of a rental.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public interface TrackingUseCase {

    Mono<PositionResponseDTO> recordPosition(UUID rentalId, PositionRequest request);

    Flux<PositionResponseDTO> getPositions(UUID rentalId);

    Mono<Double> computeTrackedKm(UUID rentalId);

    Mono<TrackingSummaryDTO> getSummary(UUID rentalId);
}
