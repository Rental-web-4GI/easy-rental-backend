package com.yowyob.easyrental.modules.rental.domain.port.out;

import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Outgoing port for rental persistence.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public interface RentalRepositoryPort {

    Mono<RentalEntity> findById(UUID id);

    Mono<RentalEntity> save(RentalEntity rental);

    Mono<RentalEntity> findExistingPendingRental(UUID clientId, UUID vehicleId);

    Flux<RentalEntity> findAllByClientIdAndStatusIn(UUID clientId, List<RentalStatus> statuses);

    Flux<RentalEntity> findAllByAgencyIdAndStatusIn(UUID agencyId, List<RentalStatus> statuses);

    Flux<RentalEntity> findAllByOrganizationIdAndStatusIn(UUID orgId, List<RentalStatus> statuses);
}
