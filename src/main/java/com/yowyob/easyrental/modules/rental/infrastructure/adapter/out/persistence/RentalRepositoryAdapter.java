package com.yowyob.easyrental.modules.rental.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Adapter implementing RentalRepositoryPort via R2DBC.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@Component
@RequiredArgsConstructor
public class RentalRepositoryAdapter implements RentalRepositoryPort {

    private final RentalRepository rentalRepository;

    @Override
    public Mono<RentalEntity> findById(UUID id) {
        return rentalRepository.findById(id);
    }

    @Override
    public Mono<RentalEntity> save(RentalEntity rental) {
        return rentalRepository.save(rental);
    }

    @Override
    public Mono<RentalEntity> findExistingPendingRental(UUID clientId, UUID vehicleId) {
        return rentalRepository.findExistingPendingRental(clientId, vehicleId);
    }

    @Override
    public Flux<RentalEntity> findAllByClientIdAndStatusIn(UUID clientId, List<RentalStatus> statuses) {
        return rentalRepository.findAllByClientIdAndStatusIn(clientId, statuses);
    }

    @Override
    public Flux<RentalEntity> findAllByAgencyIdAndStatusIn(UUID agencyId, List<RentalStatus> statuses) {
        return rentalRepository.findAllByAgencyIdAndStatusIn(agencyId, statuses);
    }

    @Override
    public Flux<RentalEntity> findAllByOrganizationIdAndStatusIn(UUID orgId, List<RentalStatus> statuses) {
        return rentalRepository.findAllByOrganizationIdAndStatusIn(orgId, statuses);
    }
}
