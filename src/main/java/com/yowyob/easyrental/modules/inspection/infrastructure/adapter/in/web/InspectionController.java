package com.yowyob.easyrental.modules.inspection.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.inspection.domain.port.in.InspectionUseCase;
import com.yowyob.easyrental.modules.inspection.dto.InspectionComparisonResult;
import com.yowyob.easyrental.modules.inspection.dto.InspectionCreateRequest;
import com.yowyob.easyrental.modules.inspection.dto.InspectionResponseDTO;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Incoming web adapter for rental inspection use cases.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
@Tag(name = "Inspections")
@SecurityRequirement(name = "bearerAuth")
public class InspectionController {

    private final InspectionUseCase useCase;

    @PostMapping("/rentals/{rentalId}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZATION','STAFF')")
    public Mono<InspectionResponseDTO> create(@PathVariable UUID rentalId, @RequestBody InspectionCreateRequest req) {
        return useCase.createInspection(rentalId, req);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZATION','STAFF','CLIENT')")
    public Mono<InspectionResponseDTO> get(@PathVariable UUID id) {
        return useCase.getInspection(id);
    }

    @GetMapping("/rentals/{rentalId}/all")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZATION','STAFF','CLIENT')")
    public Flux<InspectionResponseDTO> listByRental(@PathVariable UUID rentalId) {
        return useCase.listByRental(rentalId);
    }

    @GetMapping("/rentals/{rentalId}/comparison")
    @PreAuthorize("hasAnyRole('ADMIN','ORGANIZATION','STAFF')")
    public Mono<InspectionComparisonResult> compare(@PathVariable UUID rentalId) {
        return useCase.compareCheckInOut(rentalId);
    }
}
