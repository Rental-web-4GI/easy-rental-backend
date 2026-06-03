package com.yowyob.easyrental.modules.rental.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.rental.domain.PaymentEntity;
import com.yowyob.easyrental.modules.rental.domain.port.out.PaymentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final PaymentRepository paymentRepository;

    @Override
    public Mono<PaymentEntity> findById(UUID id) {
        return paymentRepository.findById(id);
    }

    @Override
    public Mono<PaymentEntity> save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    @Override
    public Flux<PaymentEntity> findAllByRentalId(UUID rentalId) {
        return paymentRepository.findAllByRentalId(rentalId);
    }

    @Override
    public Flux<PaymentEntity> findAllByClientId(UUID clientId) {
        return paymentRepository.findAllByClientId(clientId);
    }

    @Override
    public Flux<PaymentEntity> findAllByAgencyId(UUID agencyId) {
        return paymentRepository.findAllByAgencyId(agencyId);
    }

    @Override
    public Flux<PaymentEntity> findAllRentalPaymentsByOrganizationId(UUID orgId) {
        return paymentRepository.findAllRentalPaymentsByOrganizationId(orgId);
    }
}
