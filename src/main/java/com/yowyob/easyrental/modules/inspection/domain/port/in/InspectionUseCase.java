package com.yowyob.easyrental.modules.inspection.domain.port.in;

import com.yowyob.easyrental.modules.inspection.dto.InspectionComparisonResult;
import com.yowyob.easyrental.modules.inspection.dto.InspectionCreateRequest;
import com.yowyob.easyrental.modules.inspection.dto.InspectionResponseDTO;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for rental inspection use cases.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public interface InspectionUseCase {
    Mono<InspectionResponseDTO> createInspection(UUID rentalId, InspectionCreateRequest request);
    Mono<InspectionResponseDTO> getInspection(UUID inspectionId);
    Flux<InspectionResponseDTO> listByRental(UUID rentalId);
    Mono<InspectionComparisonResult> compareCheckInOut(UUID rentalId);
}
