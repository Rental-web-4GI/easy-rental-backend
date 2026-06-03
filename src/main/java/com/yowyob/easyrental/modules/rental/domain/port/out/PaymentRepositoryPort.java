package com.yowyob.easyrental.modules.rental.domain.port.out;

import com.yowyob.easyrental.modules.rental.domain.PaymentEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for payment persistence.
 */
public interface PaymentRepositoryPort {

    Mono<PaymentEntity> findById(UUID id);

    Mono<PaymentEntity> save(PaymentEntity payment);

    Flux<PaymentEntity> findAllByRentalId(UUID rentalId);

    Flux<PaymentEntity> findAllByClientId(UUID clientId);

    Flux<PaymentEntity> findAllByAgencyId(UUID agencyId);

    Flux<PaymentEntity> findAllRentalPaymentsByOrganizationId(UUID orgId);
}
