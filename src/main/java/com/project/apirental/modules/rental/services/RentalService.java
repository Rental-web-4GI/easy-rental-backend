package com.project.apirental.modules.rental.services;

import com.project.apirental.modules.agency.mapper.AgencyMapper;
import com.project.apirental.modules.agency.repository.AgencyRepository;
import com.project.apirental.modules.driver.dto.DriverResponseDTO;
import com.project.apirental.modules.driver.services.DriverService;
import com.project.apirental.modules.notification.domain.NotificationTemplate;
import com.project.apirental.modules.notification.services.NotificationService;
import com.project.apirental.modules.organization.repository.OrganizationRepository;
import com.project.apirental.modules.pricing.domain.PricingEntity;
import com.project.apirental.modules.pricing.services.PricingService;
import com.project.apirental.modules.rental.domain.RentalEntity;
import com.project.apirental.modules.rental.dto.*;
import com.project.apirental.modules.rental.repository.RentalRepository;
import com.project.apirental.modules.schedule.services.ScheduleService;
import com.project.apirental.modules.vehicle.repository.VehicleRepository;
import com.project.apirental.modules.vehicle.services.VehicleService;
import com.project.apirental.shared.dto.ScheduleRequestDTO;
import com.project.apirental.shared.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final VehicleRepository vehicleRepository;
    private final AgencyRepository agencyRepository;
    private final OrganizationRepository organizationRepository;
    private final PricingService pricingService;
    private final ScheduleService scheduleService;
    private final NotificationService notificationService;
    private final AgencyMapper agencyMapper;
    private final VehicleService vehicleService;
    private final DriverService driverService;

    // CORRECTION : PENDING est remis ici pour que le client puisse voir son "panier" et le payer
    private static final List<RentalStatus> RESERVATION_ACTIVE_STATUSES = Arrays.asList(RentalStatus.PENDING, RentalStatus.RESERVED, RentalStatus.PAID);
    private static final List<RentalStatus> RESERVATION_ALL_STATUSES = Arrays.asList(RentalStatus.PENDING, RentalStatus.RESERVED, RentalStatus.PAID, RentalStatus.CANCELLED);
    private static final List<RentalStatus> RENTAL_STATUSES = Arrays.asList(RentalStatus.ONGOING, RentalStatus.UNDER_REVIEW, RentalStatus.COMPLETED);

    public Mono<RentalDetailResponseDTO> getRentalDetails(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .switchIfEmpty(Mono.error(new RuntimeException("Location ou Réservation non trouvée")))
            .flatMap(rental -> {
                var vehicleMono = vehicleService.getVehicleById(rental.getVehicleId());
                var driverMono = rental.getDriverId() != null
                    ? driverService.getDriverById(rental.getDriverId()).map(Optional::of).defaultIfEmpty(Optional.empty())
                    : Mono.just(Optional.<DriverResponseDTO>empty());
                var agencyMono = agencyRepository.findById(rental.getAgencyId()).map(agencyMapper::toDto);

                return Mono.zip(vehicleMono, driverMono, agencyMono)
                    .map(tuple -> new RentalDetailResponseDTO(rental, tuple.getT1(), tuple.getT2().orElse(null), tuple.getT3()));
            });
    }

    @Transactional
    public Mono<RentalInitResponse> initiateRental(UUID clientId, RentalInitRequest request) {
        return vehicleRepository.findById(request.vehicleId())
            .switchIfEmpty(Mono.error(new RuntimeException("Véhicule non trouvé")))
            .flatMap(vehicle -> organizationRepository.findById(vehicle.getOrganizationId())
                .flatMap(org -> {
                    boolean isDriverRequired = Boolean.TRUE.equals(org.getIsDriverBookingRequired());
                    boolean hasDriverSelected = request.driverId() != null;

                    if (isDriverRequired && !hasDriverSelected) {
                        return Mono.error(new RuntimeException("La sélection d'un chauffeur est obligatoire pour cette organisation."));
                    }

                    if (!isDriverRequired && !hasDriverSelected) {
                        return agencyRepository.findById(vehicle.getAgencyId())
                            .map(agency -> new RentalInitResponse(
                                false, "Veuillez contacter l'agence directement pour une location sans chauffeur.", null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, agencyMapper.toDto(agency)
                            ));
                    }

                    return Mono.zip(
                        pricingService.getPricing(ResourceType.VEHICLE, request.vehicleId()),
                        pricingService.getPricing(ResourceType.DRIVER, request.driverId()),
                        agencyRepository.findById(vehicle.getAgencyId())
                    ).flatMap(tuple -> {
                        var vehiclePrice = tuple.getT1();
                        var driverPrice = tuple.getT2();
                        var agency = tuple.getT3();

                        long duration = (request.rentalType() == RentalType.DAILY)
                            ? Math.max(1, Duration.between(request.startDate(), request.endDate()).toDays())
                            : Math.max(1, Duration.between(request.startDate(), request.endDate()).toHours());

                        BigDecimal vPrice = (request.rentalType() == RentalType.DAILY) ? vehiclePrice.getPricePerDay() : vehiclePrice.getPricePerHour();
                        BigDecimal dPrice = (request.rentalType() == RentalType.DAILY) ? driverPrice.getPricePerDay() : driverPrice.getPricePerHour();

                        BigDecimal baseAmount = vPrice.add(dPrice).multiply(BigDecimal.valueOf(duration));
                        BigDecimal commission = baseAmount.multiply(BigDecimal.valueOf(0.01));
                        BigDecimal deposit = baseAmount.multiply(BigDecimal.valueOf(0.10));
                        BigDecimal totalFinal = baseAmount.add(commission).add(deposit);

                        // SOLUTION ANTI-DOUBLON : On cherche si une réservation PENDING existe déjà
                        return rentalRepository.findExistingPendingRental(clientId, request.vehicleId())
                            .flatMap(existingRental -> {
                                // Mise à jour de la réservation existante (Upsert)
                                existingRental.setDriverId(request.driverId());
                                existingRental.setStartDate(request.startDate());
                                existingRental.setEndDate(request.endDate());
                                existingRental.setRentalType(request.rentalType());
                                existingRental.setTotalAmount(totalFinal);
                                existingRental.setCommissionAmount(commission);
                                existingRental.setDepositAmount(deposit);
                                existingRental.setClientPhone(request.clientPhone());
                                existingRental.setUpdatedAt(LocalDateTime.now());

                                return rentalRepository.save(existingRental);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                // Création d'une nouvelle réservation si aucune n'existe
                                RentalEntity newRental = RentalEntity.builder()
                                    .id(UUID.randomUUID())
                                    .clientId(clientId)
                                    .agencyId(vehicle.getAgencyId())
                                    .vehicleId(request.vehicleId())
                                    .driverId(request.driverId())
                                    .startDate(request.startDate())
                                    .endDate(request.endDate())
                                    .status(RentalStatus.PENDING)
                                    .rentalType(request.rentalType())
                                    .totalAmount(totalFinal)
                                    .amountPaid(BigDecimal.ZERO)
                                    .commissionAmount(commission)
                                    .depositAmount(deposit)
                                    .clientPhone(request.clientPhone())
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .isNewRecord(true)
                                    .build();
                                return rentalRepository.save(newRental);
                            }))
                            .map(saved -> new RentalInitResponse(
                                true,
                                String.format(NotificationTemplate.RESERVATION_INIT_CLIENT.getTemplate(), totalFinal.multiply(BigDecimal.valueOf(0.6))),
                                saved.getId(), totalFinal, deposit, commission, agencyMapper.toDto(agency)
                            ))
                            // Notification agence : nouvelle réservation reçue
                            .flatMap(response -> notificationService.createNotification(
                                response.rentalId(), agency.getId(), NotificationResourceType.AGENCY, NotificationReason.RESERVATION_NEW,
                                request.vehicleId(), request.driverId(), NotificationTemplate.RESERVATION_INIT_AGENCY
                            ).thenReturn(response));
                    });
                }));
    }

    // Création directe par l'agence (Walk-in) avec les nouveaux champs
    @Transactional
    public Mono<RentalInitResponse> createAgencyRental(UUID agencyId, AgencyRentalRequest request) {
        return vehicleRepository.findById(request.vehicleId())
            .filter(v -> v.getAgencyId().equals(agencyId))
            .switchIfEmpty(Mono.error(new RuntimeException("Véhicule non trouvé ou n'appartient pas à cette agence")))
            .flatMap(vehicle -> Mono.zip(
                pricingService.getPricing(ResourceType.VEHICLE, request.vehicleId()),
                request.driverId() != null ? pricingService.getPricing(ResourceType.DRIVER, request.driverId()) : Mono.just(new PricingEntity()),
                agencyRepository.findById(agencyId)
            ).flatMap(tuple -> {
                var vehiclePrice = tuple.getT1();
                var driverPrice = tuple.getT2();
                var agency = tuple.getT3();

                long duration = (request.rentalType() == RentalType.DAILY)
                    ? Math.max(1, Duration.between(request.startDate(), request.endDate()).toDays())
                    : Math.max(1, Duration.between(request.startDate(), request.endDate()).toHours());

                BigDecimal vPrice = (request.rentalType() == RentalType.DAILY) ? vehiclePrice.getPricePerDay() : vehiclePrice.getPricePerHour();
                BigDecimal dPrice = (request.driverId() != null)
                    ? ((request.rentalType() == RentalType.DAILY) ? driverPrice.getPricePerDay() : driverPrice.getPricePerHour())
                    : BigDecimal.ZERO;

                BigDecimal baseAmount = vPrice.add(dPrice).multiply(BigDecimal.valueOf(duration));
                BigDecimal commission = baseAmount.multiply(BigDecimal.valueOf(0.01));
                BigDecimal deposit = baseAmount.multiply(BigDecimal.valueOf(0.10));
                BigDecimal totalFinal = baseAmount.add(commission).add(deposit);

                RentalEntity rental = RentalEntity.builder()
                    .id(UUID.randomUUID())
                    .clientName(request.clientName())
                    .clientPhone(request.clientPhone())
                    .clientEmail(request.clientEmail()) // NOUVEAU
                    .cniNumber(request.cniNumber())     // NOUVEAU
                    .agencyId(agencyId)
                    .vehicleId(request.vehicleId())
                    .driverId(request.driverId())
                    .startDate(request.startDate())
                    .endDate(request.endDate())
                    .status(RentalStatus.PENDING)
                    .rentalType(request.rentalType())
                    .totalAmount(totalFinal)
                    .amountPaid(BigDecimal.ZERO)
                    .commissionAmount(commission)
                    .depositAmount(deposit)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isNewRecord(true)
                    .build();

                return rentalRepository.save(rental)
                    .map(saved -> new RentalInitResponse(
                        true, "Location agence créée. Veuillez procéder à l'encaissement.",
                        saved.getId(), totalFinal, deposit, commission, agencyMapper.toDto(agency)
                    ));
            }));
    }

    @Transactional
    public Mono<RentalEntity> startRental(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .filter(r -> r.getStatus() == RentalStatus.PAID)
            .switchIfEmpty(Mono.error(new RuntimeException("La location doit être entièrement payée (PAID) pour démarrer.")))
            .flatMap(rental -> {
                rental.setStatus(RentalStatus.ONGOING);
                rental.setUpdatedAt(LocalDateTime.now());
                return rentalRepository.save(rental)
                    .flatMap(saved -> Mono.when(
                        saved.getClientId() != null ? notificationService.createNotification(
                            saved.getId(), saved.getClientId(), NotificationResourceType.CLIENT, NotificationReason.LOCATION_START,
                            saved.getVehicleId(), saved.getDriverId(), NotificationTemplate.LOCATION_START_CLIENT
                        ) : Mono.empty(),
                        notificationService.createNotification(
                            saved.getId(), saved.getAgencyId(), NotificationResourceType.AGENCY, NotificationReason.LOCATION_START,
                            saved.getVehicleId(), saved.getDriverId(), NotificationTemplate.LOCATION_START_AGENCY
                        )
                    ).thenReturn(saved));
            });
    }

    @Transactional
    public Mono<RentalEntity> signalEndRental(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .filter(r -> r.getStatus() == RentalStatus.ONGOING)
            .flatMap(rental -> {
                rental.setStatus(RentalStatus.UNDER_REVIEW);
                rental.setUpdatedAt(LocalDateTime.now());
                return rentalRepository.save(rental)
                    .flatMap(saved -> notificationService.createNotification(
                        saved.getId(), saved.getAgencyId(), NotificationResourceType.AGENCY, NotificationReason.LOCATION_END_SIGNAL,
                        saved.getVehicleId(), saved.getDriverId(), NotificationTemplate.LOCATION_END_SIGNAL_AGENCY
                    ).thenReturn(saved));
            });
    }

    @Transactional
    public Mono<RentalEntity> validateReturn(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .filter(r -> r.getStatus() == RentalStatus.UNDER_REVIEW)
            .flatMap(rental -> {
                rental.setStatus(RentalStatus.COMPLETED);
                rental.setUpdatedAt(LocalDateTime.now());

                LocalDateTime maintenanceEnd = rental.getEndDate().plusHours(24);
                ScheduleRequestDTO schedule = new ScheduleRequestDTO(
                    rental.getEndDate(), maintenanceEnd, "MAINTENANCE", "Révision post-location"
                );

                return Mono.when(
                    scheduleService.addUnavailability(rental.getAgencyId(), ResourceType.VEHICLE, rental.getVehicleId(), schedule),
                    rental.getDriverId() != null ? scheduleService.addUnavailability(rental.getAgencyId(), ResourceType.DRIVER, rental.getDriverId(), schedule) : Mono.empty()
                ).then(rentalRepository.save(rental))
                 .flatMap(saved -> Mono.when(
                     saved.getClientId() != null ? notificationService.createNotification(
                         saved.getId(), saved.getClientId(), NotificationResourceType.CLIENT, NotificationReason.LOCATION_END,
                         saved.getVehicleId(), saved.getDriverId(), NotificationTemplate.LOCATION_END_VALIDATED_CLIENT
                     ) : Mono.empty(),
                     notificationService.createNotification(
                         saved.getId(), saved.getAgencyId(), NotificationResourceType.AGENCY, NotificationReason.LOCATION_END,
                         saved.getVehicleId(), saved.getDriverId(), NotificationTemplate.LOCATION_END_VALIDATED_AGENCY
                     )
                 ).thenReturn(saved));
            });
    }

    @Transactional
    public Mono<RentalEntity> cancelRental(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .filter(r -> r.getStatus() == RentalStatus.RESERVED || r.getStatus() == RentalStatus.PAID || r.getStatus() == RentalStatus.PENDING)
            .switchIfEmpty(Mono.error(new RuntimeException("Impossible d'annuler cette réservation.")))
            .flatMap(rental -> {
                BigDecimal amountPaid = rental.getAmountPaid();
                BigDecimal penalty = amountPaid.multiply(BigDecimal.valueOf(0.05));
                BigDecimal refundAmount = amountPaid.subtract(penalty);

                rental.setStatus(RentalStatus.CANCELLED);
                rental.setUpdatedAt(LocalDateTime.now());

                Mono<Void> freeSchedule = scheduleService.removeScheduleForRental(rental.getVehicleId(), rental.getDriverId(), rental.getStartDate(), rental.getEndDate());

                return rentalRepository.save(rental)
                    .flatMap(saved -> freeSchedule
                        .then(Mono.when(
                            saved.getClientId() != null ? notificationService.createNotification(
                                saved.getId(), saved.getClientId(), NotificationResourceType.CLIENT, NotificationReason.CANCELLATION,
                                saved.getVehicleId(), saved.getDriverId(),
                                NotificationTemplate.CANCELLATION_CLIENT, amountPaid, penalty, refundAmount
                            ) : Mono.empty(),
                            notificationService.createNotification(
                                saved.getId(), saved.getAgencyId(), NotificationResourceType.AGENCY, NotificationReason.CANCELLATION,
                                saved.getVehicleId(), saved.getDriverId(),
                                NotificationTemplate.CANCELLATION_AGENCY, penalty
                            )
                        ))
                        .thenReturn(saved)
                    );
            });
    }

    public Flux<RentalEntity> getClientActiveReservations(UUID clientId) {
        return rentalRepository.findAllByClientIdAndStatusIn(clientId, RESERVATION_ACTIVE_STATUSES);
    }
    public Flux<RentalEntity> getClientRentalsHistory(UUID clientId) {
        return rentalRepository.findAllByClientIdAndStatusIn(clientId, RENTAL_STATUSES);
    }
    public Flux<RentalEntity> getAgencyReservations(UUID agencyId) {
        return rentalRepository.findAllByAgencyIdAndStatusIn(agencyId, RESERVATION_ALL_STATUSES);
    }
    public Flux<RentalEntity> getAgencyRentals(UUID agencyId) {
        return rentalRepository.findAllByAgencyIdAndStatusIn(agencyId, RENTAL_STATUSES);
    }
    public Flux<RentalEntity> getOrganizationReservations(UUID orgId) {
        return rentalRepository.findAllByOrganizationIdAndStatusIn(orgId, RESERVATION_ALL_STATUSES);
    }
    public Flux<RentalEntity> getOrganizationRentals(UUID orgId) {
        return rentalRepository.findAllByOrganizationIdAndStatusIn(orgId, RENTAL_STATUSES);
    }
}
