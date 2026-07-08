package com.yowyob.easyrental.modules.review.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.review.dto.FeaturedReviewsResponseDTO;
import com.yowyob.easyrental.modules.review.dto.PlatformFeedbackRequestDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewModerationStatsDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewPublishRequestDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewRequestDTO;
import com.yowyob.easyrental.modules.review.dto.ReviewResponseDTO;
import com.yowyob.easyrental.modules.review.domain.port.in.ReviewUseCase;
import com.yowyob.easyrental.shared.enums.ResourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Review Management", description = "Gestion des avis sur les véhicules et chauffeurs")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final ReviewUseCase reviewUseCase;

    @Operation(summary = "Ajouter un avis après location")
    @PostMapping
    public Mono<ResponseEntity<ReviewResponseDTO>> addReview(@RequestBody @Valid ReviewRequestDTO request) {
        return reviewUseCase.addReview(request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Feedback landing page (public)")
    @PostMapping("/platform-feedback")
    public Mono<ResponseEntity<ReviewResponseDTO>> addPlatformFeedback(
            @RequestBody @Valid PlatformFeedbackRequestDTO request) {
        return reviewUseCase.addPlatformFeedback(request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Avis publiés pour la landing page")
    @GetMapping("/featured")
    public Mono<ResponseEntity<FeaturedReviewsResponseDTO>> getFeaturedReviews() {
        return reviewUseCase.getFeaturedReviews(6)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les avis d'une ressource")
    @GetMapping("/{type}/{id}")
    public Flux<ReviewResponseDTO> getReviews(
            @PathVariable ResourceType type,
            @PathVariable UUID id) {
        return reviewUseCase.getReviews(type, id);
    }

    @Operation(summary = "Statistiques modération (admin)")
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ReviewModerationStatsDTO>> getModerationStats() {
        return reviewUseCase.getModerationStats()
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister tous les avis (admin)")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<ReviewResponseDTO> listAllForAdmin() {
        return reviewUseCase.listAllForAdmin();
    }

    @Operation(summary = "Publier ou dépublier un avis (admin)")
    @PatchMapping("/admin/{id}/published")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ReviewResponseDTO>> setPublished(
            @PathVariable UUID id,
            @RequestBody @Valid ReviewPublishRequestDTO request) {
        return reviewUseCase.setPublished(id, request.published())
                .map(ResponseEntity::ok);
    }
}
