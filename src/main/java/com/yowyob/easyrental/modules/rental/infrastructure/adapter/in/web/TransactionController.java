package com.yowyob.easyrental.modules.rental.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence.UserRepository;
import com.yowyob.easyrental.modules.rental.dto.TransactionDetailResponseDTO;
import com.yowyob.easyrental.modules.rental.dto.TransactionResponseDTO;
import com.yowyob.easyrental.modules.rental.application.TransactionUseCaseImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Financial Transactions", description = "Historique des paiements et abonnements")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionUseCaseImpl transactionUseCaseImpl;
    private final UserRepository userRepository;

    @Operation(summary = "Détails d'une transaction (Client & Agence/Org)")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<TransactionDetailResponseDTO>> getTransactionDetails(@PathVariable UUID id) {
        return transactionUseCaseImpl.getTransactionDetails(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "CLIENT: Mes transactions (Historique paiements)")
    @GetMapping("/client/history")
    public Flux<TransactionResponseDTO> getMyTransactions() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(userRepository::findByEmail)
            .flatMapMany(user -> transactionUseCaseImpl.getClientTransactions(user.getId()));
    }

    @Operation(summary = "AGENCE: Historique des transactions (Revenus)")
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Flux<TransactionResponseDTO> getAgencyTransactions(@PathVariable UUID agencyId) {
        return transactionUseCaseImpl.getAgencyTransactions(agencyId);
    }

    @Operation(summary = "ORGANISATION: Grand livre (Revenus Agences + Coûts Abonnements)")
    @GetMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<TransactionResponseDTO> getOrganizationTransactions(@PathVariable UUID orgId) {
        return transactionUseCaseImpl.getOrganizationTransactions(orgId);
    }
}
