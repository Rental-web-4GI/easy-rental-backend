package com.yowyob.easyrental.modules.rental.domain.port.in;

import com.yowyob.easyrental.modules.rental.dto.TransactionDetailResponseDTO;
import com.yowyob.easyrental.modules.rental.dto.TransactionResponseDTO;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for transaction use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface TransactionUseCase {
    Mono<TransactionDetailResponseDTO> getTransactionDetails(UUID transactionId);
    Flux<TransactionResponseDTO> getClientTransactions(UUID clientId);
    Flux<TransactionResponseDTO> getAgencyTransactions(UUID agencyId);
    Flux<TransactionResponseDTO> getOrganizationTransactions(UUID orgId);
    Flux<TransactionResponseDTO> getClientTransactionsByEmail(String email);
}
