package com.yowyob.easyrental.modules.rental.domain.port.in;

import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.dto.PaymentRequest;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Incoming port for rentalpayment use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface RentalPaymentUseCase {
    Mono<RentalEntity> processPayment(UUID rentalId, PaymentRequest request);
}
