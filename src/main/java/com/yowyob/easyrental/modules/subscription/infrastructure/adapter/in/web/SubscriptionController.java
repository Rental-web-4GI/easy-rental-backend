package com.yowyob.easyrental.modules.subscription.infrastructure.adapter.in.web;

import java.util.Objects;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionPlanRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Catalog", description = "Gestion du catalogue des plans")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionPlanRepository planRepository;

    @Operation(summary = "Lister tous les plans du catalogue")
    @GetMapping("/plans")
    public Flux<SubscriptionPlanEntity> getAllPlans() {
        return planRepository.findAll();
    }

    @Operation(summary = "Mettre à jour les quotas d'un plan (Admin uniquement)")
    @PutMapping("/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Protection Admin pour le catalogue
    public Mono<ResponseEntity<SubscriptionPlanEntity>> updatePlan(
            @PathVariable UUID id, 
            @RequestBody SubscriptionPlanEntity planUpdate) {
        return planRepository.findById(Objects.requireNonNull(id, "L'ID ne peut pas être nul"))
                .flatMap(existingPlan -> {
                    UUID existingId = existingPlan.getId();
                    if (existingId != null) {
                        planUpdate.setId(existingPlan.getId());
                    }
                    return planRepository.save(Objects.requireNonNull(planUpdate));
                })
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new RuntimeException("Plan non trouvé")));
    }
}