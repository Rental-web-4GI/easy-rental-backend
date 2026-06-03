package com.yowyob.easyrental.modules.review.infrastructure.adapter.in.web;

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
import org.springframework.web.bind.annotation.GetMapping;
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

    @Operation(summary = "Ajouter un avis")
    @PostMapping
    public Mono<ResponseEntity<ReviewResponseDTO>> addReview(@RequestBody @Valid ReviewRequestDTO request) {
        return reviewUseCase.addReview(request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les avis d'une ressource")
    @GetMapping("/{type}/{id}")
    public Flux<ReviewResponseDTO> getReviews(
            @PathVariable ResourceType type,
            @PathVariable UUID id) {
        return reviewUseCase.getReviews(type, id);
    }
}
