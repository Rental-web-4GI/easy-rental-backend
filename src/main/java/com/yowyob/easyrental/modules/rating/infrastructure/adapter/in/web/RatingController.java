package com.yowyob.easyrental.modules.rating.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.rating.domain.port.in.RatingUseCase;
import com.yowyob.easyrental.modules.rating.dto.RatingCreateRequest;
import com.yowyob.easyrental.modules.rating.dto.RatingResponseDTO;
import com.yowyob.easyrental.modules.rating.dto.RatingStatsDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Incoming web adapter for post-rental rating use cases.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Tag(name = "Ratings")
@SecurityRequirement(name = "bearerAuth")
public class RatingController {

    private final RatingUseCase useCase;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT','ORGANIZATION','STAFF')")
    public Mono<RatingResponseDTO> submit(@RequestBody RatingCreateRequest req) {
        return useCase.submitRating(req);
    }

    @GetMapping("/agencies/{agencyId}")
    @PreAuthorize("permitAll()")
    public Flux<RatingResponseDTO> agencyRatings(@PathVariable UUID agencyId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return useCase.getRatingsForTarget("AGENCY", agencyId, page, size);
    }

    @GetMapping("/agencies/{agencyId}/stats")
    @PreAuthorize("permitAll()")
    public Mono<RatingStatsDTO> agencyStats(@PathVariable UUID agencyId) {
        return useCase.getStatsForTarget("AGENCY", agencyId);
    }
}
