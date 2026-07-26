package com.yowyob.easyrental.modules.loyalty.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyLedgerEntity;
import com.yowyob.easyrental.modules.loyalty.domain.port.in.LoyaltyUseCase;
import com.yowyob.easyrental.modules.loyalty.dto.LoyaltyBalanceDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Incoming web adapter for the loyalty points use cases.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
@Tag(name = "Loyalty")
@SecurityRequirement(name = "bearerAuth")
public class LoyaltyController {

    private final LoyaltyUseCase useCase;

    @GetMapping("/balance/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT','ORGANIZATION','STAFF')")
    public Mono<LoyaltyBalanceDTO> balance(@PathVariable UUID clientId) {
        return useCase.getBalance(clientId);
    }

    @GetMapping("/history/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT','ORGANIZATION','STAFF')")
    public Flux<LoyaltyLedgerEntity> history(@PathVariable UUID clientId) {
        return useCase.history(clientId);
    }
}
