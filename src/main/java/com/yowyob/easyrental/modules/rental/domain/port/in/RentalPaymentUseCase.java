package com.yowyob.easyrental.modules.rental.domain.port.in;

import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.dto.PaymentRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Incoming port for rental payment use cases.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public interface RentalPaymentUseCase {

    /**
     * Processes a payment for a rental.
     *
     * @param rentalId rental identifier
     * @param request payment details
     * @return updated rental entity
     */
    Mono<RentalEntity> processPayment(UUID rentalId, PaymentRequest request);
}
