package com.yowyob.easyrental.modules.tracking.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.tracking.domain.port.in.TrackingUseCase;
import com.yowyob.easyrental.modules.tracking.dto.PositionRequest;
import com.yowyob.easyrental.modules.tracking.dto.PositionResponseDTO;
import com.yowyob.easyrental.modules.tracking.dto.TrackingSummaryDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Incoming web adapter for GPS tracking use cases.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Tracking")
@SecurityRequirement(name = "bearerAuth")
public class TrackingController {

    private final TrackingUseCase useCase;

    @PostMapping("/rentals/{rentalId}/positions")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZATION','STAFF','CLIENT')")
    public Mono<PositionResponseDTO> record(@PathVariable UUID rentalId, @RequestBody PositionRequest req) {
        return useCase.recordPosition(rentalId, req);
    }

    @GetMapping("/rentals/{rentalId}/tracking")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZATION','STAFF','CLIENT')")
    public Mono<TrackingSummaryDTO> getSummary(@PathVariable UUID rentalId) {
        return useCase.getSummary(rentalId);
    }
}
