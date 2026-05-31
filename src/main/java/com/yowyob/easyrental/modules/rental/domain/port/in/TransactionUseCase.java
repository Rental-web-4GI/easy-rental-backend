package com.yowyob.easyrental.modules.rental.domain.port.in;

import com.yowyob.easyrental.modules.rental.dto.TransactionDetailResponseDTO;
import com.yowyob.easyrental.modules.rental.dto.TransactionResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Incoming port for transaction use cases.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public interface TransactionUseCase {

    Mono<TransactionDetailResponseDTO> getTransactionDetails(UUID transactionId);

    Flux<TransactionResponseDTO> getClientTransactions(UUID clientId);

    Flux<TransactionResponseDTO> getAgencyTransactions(UUID agencyId);

    Flux<TransactionResponseDTO> getOrganizationTransactions(UUID orgId);
}
