package com.yowyob.easyrental.modules.rental.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.auth.domain.port.out.AuthUserPort;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.domain.port.in.RentalPaymentUseCase;
import com.yowyob.easyrental.modules.rental.domain.port.in.RentalUseCase;
import com.yowyob.easyrental.modules.rental.dto.AgencyRentalRequest;
import com.yowyob.easyrental.modules.rental.dto.PaymentRequest;
import com.yowyob.easyrental.modules.rental.dto.RentalDetailResponseDTO;
import com.yowyob.easyrental.modules.rental.dto.RentalInitRequest;
import com.yowyob.easyrental.modules.rental.dto.RentalInitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST adapter for rental operations.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Tag(name = "Rental Process", description = "Rental reservation, payment and lifecycle")
@SecurityRequirement(name = "bearerAuth")
public class RentalController {

    private final RentalUseCase rentalUseCase;
    private final RentalPaymentUseCase rentalPaymentUseCase;
    private final AuthUserPort authUserPort;

    @Operation(summary = "Get full rental or reservation details")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<RentalDetailResponseDTO>> getRentalDetails(@PathVariable UUID id) {
        return rentalUseCase.getRentalDetails(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Initiate a rental (quote and validation)")
    @PostMapping("/init")
    public Mono<ResponseEntity<RentalInitResponse>> initiateRental(@RequestBody @Valid RentalInitRequest request) {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(authUserPort::findByEmail)
            .flatMap(user -> rentalUseCase.initiateRental(user.getId(), request))
            .map(ResponseEntity::ok);
    }

    @Operation(summary = "Create agency walk-in rental")
    @PostMapping("/agency/{agencyId}/create")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<RentalInitResponse>> createAgencyRental(
            @PathVariable UUID agencyId,
            @RequestBody @Valid AgencyRentalRequest request) {
        return rentalUseCase.createAgencyRental(agencyId, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Pay rental deposit or balance")
    @PostMapping("/{id}/pay")
    public Mono<ResponseEntity<RentalEntity>> payRental(
            @PathVariable UUID id,
            @RequestBody @Valid PaymentRequest request) {
        return rentalPaymentUseCase.processPayment(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Start rental (vehicle pickup)")
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<RentalEntity>> startRental(@PathVariable UUID id) {
        return rentalUseCase.startRental(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Signal rental end (client)")
    @PutMapping("/{id}/end-signal")
    public Mono<ResponseEntity<RentalEntity>> signalEnd(@PathVariable UUID id) {
        return rentalUseCase.signalEndRental(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Validate vehicle return (agency)")
    @PutMapping("/{id}/validate-return")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<RentalEntity>> validateReturn(@PathVariable UUID id) {
        return rentalUseCase.validateReturn(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Cancel reservation")
    @PutMapping("/{id}/cancel")
    public Mono<ResponseEntity<RentalEntity>> cancelRental(@PathVariable UUID id) {
        return rentalUseCase.cancelRental(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Client active reservations")
    @GetMapping("/client/reservations/active")
    public Flux<RentalEntity> getClientActiveReservations() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(authUserPort::findByEmail)
            .flatMapMany(user -> rentalUseCase.getClientActiveReservations(user.getId()));
    }

    @Operation(summary = "Client rental history")
    @GetMapping("/client/rentals/history")
    public Flux<RentalEntity> getClientRentalsHistory() {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(authUserPort::findByEmail)
            .flatMapMany(user -> rentalUseCase.getClientRentalsHistory(user.getId()));
    }

    @Operation(summary = "Agency reservations")
    @GetMapping("/agency/{agencyId}/reservations")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Flux<RentalEntity> getAgencyReservations(@PathVariable UUID agencyId) {
        return rentalUseCase.getAgencyReservations(agencyId);
    }

    @Operation(summary = "Agency rentals")
    @GetMapping("/agency/{agencyId}/rentals")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Flux<RentalEntity> getAgencyRentals(@PathVariable UUID agencyId) {
        return rentalUseCase.getAgencyRentals(agencyId);
    }

    @Operation(summary = "Organization reservations")
    @GetMapping("/org/{orgId}/reservations")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<RentalEntity> getOrgReservations(@PathVariable UUID orgId) {
        return rentalUseCase.getOrganizationReservations(orgId);
    }

    @Operation(summary = "Organization rentals")
    @GetMapping("/org/{orgId}/rentals")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<RentalEntity> getOrgRentals(@PathVariable UUID orgId) {
        return rentalUseCase.getOrganizationRentals(orgId);
    }
}
