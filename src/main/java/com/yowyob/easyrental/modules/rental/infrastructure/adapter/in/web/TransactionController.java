package com.yowyob.easyrental.modules.rental.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.rental.domain.port.in.TransactionUseCase;
import com.yowyob.easyrental.modules.rental.dto.TransactionDetailResponseDTO;
import com.yowyob.easyrental.modules.rental.dto.TransactionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Financial Transactions", description = "Historique des paiements et abonnements")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionUseCase transactionUseCase;

    @Operation(summary = "Détails d'une transaction (Client & Agence/Org)")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<TransactionDetailResponseDTO>> getTransactionDetails(@PathVariable UUID id) {
        return transactionUseCase.getTransactionDetails(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "CLIENT: Mes transactions (Historique paiements)")
    @GetMapping("/client/history")
    public Flux<TransactionResponseDTO> getMyTransactions() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMapMany(transactionUseCase::getClientTransactionsByEmail);
    }

    @Operation(summary = "AGENCE: Historique des transactions (Revenus)")
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Flux<TransactionResponseDTO> getAgencyTransactions(@PathVariable UUID agencyId) {
        return transactionUseCase.getAgencyTransactions(agencyId);
    }

    @Operation(summary = "ORGANISATION: Grand livre (Revenus Agences + Coûts Abonnements)")
    @GetMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<TransactionResponseDTO> getOrganizationTransactions(@PathVariable UUID orgId) {
        return transactionUseCase.getOrganizationTransactions(orgId);
    }
}
