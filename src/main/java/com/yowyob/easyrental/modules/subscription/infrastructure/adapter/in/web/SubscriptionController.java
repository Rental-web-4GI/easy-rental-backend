package com.yowyob.easyrental.modules.subscription.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Catalog", description = "Gestion du catalogue des plans")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final SubscriptionUseCase subscriptionUseCase;

    @Operation(summary = "Lister tous les plans du catalogue")
    @GetMapping("/plans")
    public Flux<SubscriptionPlanEntity> getAllPlans() {
        return subscriptionUseCase.getAllPlans();
    }

    @Operation(summary = "Mettre à jour les quotas d'un plan (Admin uniquement)")
    @PutMapping("/plans/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<SubscriptionPlanEntity>> updatePlan(
            @PathVariable UUID id,
            @RequestBody SubscriptionPlanEntity planUpdate) {
        return subscriptionUseCase.updatePlan(id, planUpdate).map(ResponseEntity::ok);
    }
}
