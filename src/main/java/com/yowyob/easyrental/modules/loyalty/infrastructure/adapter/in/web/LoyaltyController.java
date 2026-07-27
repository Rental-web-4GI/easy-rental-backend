package com.yowyob.easyrental.modules.loyalty.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.auth.domain.port.out.AuthUserPort;
import com.yowyob.easyrental.modules.loyalty.domain.LoyaltyLedgerEntity;
import com.yowyob.easyrental.modules.loyalty.domain.port.in.LoyaltyUseCase;
import com.yowyob.easyrental.modules.loyalty.dto.LoyaltyBalanceDTO;
import com.yowyob.easyrental.shared.exception.ValidationException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
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
    private final AuthUserPort authUserPort;

    @GetMapping("/balance/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT','ORGANIZATION','STAFF')")
    public Mono<LoyaltyBalanceDTO> balance(@PathVariable UUID clientId) {
        return assertCanAccess(clientId).then(useCase.getBalance(clientId));
    }

    @GetMapping("/history/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENT','ORGANIZATION','STAFF')")
    public Flux<LoyaltyLedgerEntity> history(@PathVariable UUID clientId) {
        return assertCanAccess(clientId).thenMany(useCase.history(clientId));
    }

    /**
     * A CLIENT may only read its own balance/history ; the business roles
     * (ADMIN / ORGANIZATION / STAFF) operate on behalf of an agency and may
     * look up a given client. Prevents client-to-client IDOR.
     */
    private Mono<Void> assertCanAccess(UUID clientId) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(authUserPort::findByEmail)
                .flatMap(user -> {
                    boolean isClient = "CLIENT".equalsIgnoreCase(user.getRole());
                    if (isClient && !clientId.equals(user.getId())) {
                        return Mono.error(new ValidationException("FORBIDDEN_LOYALTY_ACCESS"));
                    }
                    return Mono.empty();
                });
    }
}
