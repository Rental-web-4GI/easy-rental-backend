package com.yowyob.easyrental.modules.rental.domain.port.in;

import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.dto.AgencyRentalRequest;
import com.yowyob.easyrental.modules.rental.dto.RentalDetailResponseDTO;
import com.yowyob.easyrental.modules.rental.dto.RentalInitRequest;
import com.yowyob.easyrental.modules.rental.dto.RentalInitResponse;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for rental use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface RentalUseCase {
    Mono<RentalDetailResponseDTO> getRentalDetails(UUID rentalId);
    Mono<RentalInitResponse> initiateRental(UUID clientId, RentalInitRequest request);
    Mono<RentalInitResponse> createAgencyRental(UUID agencyId, AgencyRentalRequest request);
    Mono<RentalEntity> startRental(UUID rentalId);
    Mono<RentalEntity> signalEndRental(UUID rentalId);
    Mono<RentalEntity> validateReturn(UUID rentalId);
    Mono<RentalEntity> cancelRental(UUID rentalId);
    Flux<RentalEntity> getClientActiveReservations(UUID clientId);
    Flux<RentalEntity> getClientRentalsHistory(UUID clientId);
    Flux<RentalEntity> getAgencyReservations(UUID agencyId);
    Flux<RentalEntity> getAgencyRentals(UUID agencyId);
    Flux<RentalEntity> getOrganizationReservations(UUID orgId);
    Flux<RentalEntity> getOrganizationRentals(UUID orgId);
}
