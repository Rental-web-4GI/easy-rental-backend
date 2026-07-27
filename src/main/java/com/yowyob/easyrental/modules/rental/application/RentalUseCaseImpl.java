package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.auth.domain.port.out.AuthUserPort;
import com.yowyob.easyrental.modules.agency.mapper.AgencyMapper;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.driver.domain.port.in.DriverUseCase;
import com.yowyob.easyrental.modules.driver.dto.DriverResponseDTO;
import com.yowyob.easyrental.modules.notification.domain.NotificationTemplate;
import com.yowyob.easyrental.modules.notification.domain.port.in.NotificationUseCase;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.pricing.domain.port.in.PricingUseCase;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.domain.PaymentEntity;
import com.yowyob.easyrental.modules.rental.dto.AgencyRentalRequest;
import com.yowyob.easyrental.modules.rental.dto.CheckInRequest;
import com.yowyob.easyrental.modules.rental.dto.CheckOutRequest;
import com.yowyob.easyrental.modules.rental.dto.CheckoutSettlementRequest;
import com.yowyob.easyrental.modules.rental.dto.RentalDetailResponseDTO;
import com.yowyob.easyrental.modules.rental.dto.PaymentRequest;
import com.yowyob.easyrental.modules.rental.dto.RentalInitRequest;
import com.yowyob.easyrental.modules.rental.dto.RentalInitResponse;
import com.yowyob.easyrental.modules.rental.dto.RentalPricingBreakdown;
import com.yowyob.easyrental.shared.dto.ScheduleRequestDTO;
import com.yowyob.easyrental.modules.rental.domain.port.in.RentalPaymentUseCase;
import com.yowyob.easyrental.modules.rental.domain.port.in.RentalUseCase;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.modules.rental.domain.port.out.PaymentRepositoryPort;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalEmailPort;
import com.yowyob.easyrental.modules.inspection.domain.port.in.InspectionUseCase;
import com.yowyob.easyrental.modules.inspection.dto.InspectionCreateRequest;
import com.yowyob.easyrental.modules.tracking.domain.port.in.TrackingUseCase;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import com.yowyob.easyrental.modules.vehicle.domain.port.in.VehicleUseCase;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import com.yowyob.easyrental.shared.constants.RentalConstants;
import com.yowyob.easyrental.shared.exception.RentalConflictException;
import com.yowyob.easyrental.shared.exception.ResourceNotFoundException;
import com.yowyob.easyrental.shared.exception.ValidationException;
import com.yowyob.easyrental.shared.enums.NotificationReason;
import com.yowyob.easyrental.shared.enums.NotificationResourceType;
import com.yowyob.easyrental.shared.enums.PaymentMethod;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import com.yowyob.easyrental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RentalUseCaseImpl implements RentalUseCase {

    private final RentalRepositoryPort rentalRepository;
    private final VehicleRepositoryPort vehicleRepository;
    private final AgencyRepositoryPort agencyRepository;
    private final OrganizationRepositoryPort organizationRepository;
    private final PricingUseCase pricingService;
    private final ScheduleUseCase scheduleService;
    private final NotificationUseCase notificationService;
    private final RentalPaymentUseCase rentalPaymentUseCase;
    private final AgencyMapper agencyMapper;
    private final VehicleUseCase vehicleService;
    private final DriverUseCase driverService;
    private final AuthUserPort authUserPort;
    private final PaymentRepositoryPort paymentRepository;
    private final InspectionUseCase inspectionUseCase;
    private final TrackingUseCase trackingUseCase;
    private final RentalEmailPort rentalEmailPort;
    private final com.yowyob.easyrental.modules.loyalty.domain.port.in.LoyaltyUseCase loyaltyUseCase;

    // CORRECTION : PENDING est remis ici pour que le client puisse voir son "panier" et le payer
    private static final List<RentalStatus> RESERVATION_ACTIVE_STATUSES = Arrays.asList(
            RentalStatus.PENDING, RentalStatus.RESERVED, RentalStatus.PAID);
    private static final List<RentalStatus> RESERVATION_ALL_STATUSES = Arrays.asList(
            RentalStatus.PENDING, RentalStatus.RESERVED, RentalStatus.PAID, RentalStatus.CANCELLED);
    private static final List<RentalStatus> RENTAL_STATUSES = Arrays.asList(
            RentalStatus.ONGOING, RentalStatus.UNDER_REVIEW, RentalStatus.COMPLETED);

    public Mono<RentalDetailResponseDTO> getRentalDetails(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rental or reservation not found")))
            .flatMap(rental -> {
                var vehicleMono = vehicleService.getVehicleById(rental.getVehicleId())
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty());
                var driverMono = rental.getDriverId() != null
                    ? driverService.getDriverById(rental.getDriverId())
                        .map(Optional::of).defaultIfEmpty(Optional.empty())
                    : Mono.just(Optional.<DriverResponseDTO>empty());
                var agencyMono = agencyRepository.findById(rental.getAgencyId())
                    .map(agencyMapper::toDto)
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty());

                return Mono.zip(vehicleMono, driverMono, agencyMono)
                    .map(tuple -> new RentalDetailResponseDTO(
                            rental,
                            tuple.getT1().orElse(null),
                            tuple.getT2().orElse(null),
                            tuple.getT3().orElse(null)));
            });
    }

    @Transactional
    public Mono<RentalInitResponse> initiateRental(UUID clientId, RentalInitRequest request) {
        return validateRentalWindow(request.startDate(), request.endDate())
            .then(Mono.defer(() -> authUserPort.findById(clientId)))
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Client not found")))
            .flatMap(client -> vehicleRepository.findById(request.vehicleId())
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Vehicle not found")))
            .flatMap(vehicle -> organizationRepository.findById(vehicle.getOrganizationId())
                .flatMap(org -> {
                    boolean isDriverRequired = Boolean.TRUE.equals(org.getIsDriverBookingRequired());
                    boolean hasDriverSelected = request.driverId() != null;
                    String clientLabel = RentalClientLabelResolver.resolveFromUser(client);

                    if (isDriverRequired && !hasDriverSelected) {
                        return Mono.error(new ValidationException(
                                "Driver selection is required for this organization."));
                    }

                    return Mono.zip(
                        pricingService.getPricing(ResourceType.VEHICLE, request.vehicleId()),
                        hasDriverSelected
                            ? pricingService.getPricing(ResourceType.DRIVER, request.driverId())
                            : Mono.just(new PricingEntity()),
                        agencyRepository.findById(vehicle.getAgencyId())
                    ).flatMap(tuple -> {
                        var vehiclePrice = tuple.getT1();
                        var driverPrice = tuple.getT2();
                        var agency = tuple.getT3();

                        BigDecimal rawBase = RentalDurationCalculator.computeBaseAmount(
                            request.startDate(), request.endDate(), request.rentalType(),
                            vehiclePrice, hasDriverSelected ? driverPrice : null);
                        // R3 : remise fidélité = min(points × 10, 50% de la base location).
                        int redeemPts = request.redeemPoints() == null ? 0 : Math.max(0, request.redeemPoints());
                        BigDecimal loyaltyDiscount = redeemPts > 0
                            ? BigDecimal.valueOf((long) redeemPts * 10)
                                .min(rawBase.multiply(new BigDecimal("0.5")))
                                .setScale(2, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                        BigDecimal baseAmount = rawBase.subtract(loyaltyDiscount).max(BigDecimal.ZERO);
                        RentalPricingBreakdown breakdown = RentalPricingCalculator.computeBreakdown(
                            baseAmount, RentalConstants.PLATFORM_COMMISSION_RATE, agency.getDepositPercentage());
                        BigDecimal commission = breakdown.commissionAmount();
                        BigDecimal deposit = breakdown.requestedUpfront();
                        BigDecimal totalFinal = breakdown.totalDue();

                        // R3 : on débite les points d'abord (échoue si solde insuffisant) puis on crée la résa.
                        Mono<Void> doRedeem = redeemPts > 0
                            ? loyaltyUseCase.redeem(clientId, redeemPts, null).then()
                            : Mono.empty();

                        // SOLUTION ANTI-DOUBLON : On cherche si une réservation PENDING existe déjà
                        return doRedeem.then(rentalRepository.findExistingPendingRental(clientId, request.vehicleId())
                            .flatMap(existingRental -> {
                                // Mise à jour de la réservation existante (Upsert)
                                existingRental.setDriverId(request.driverId());
                                existingRental.setStartDate(request.startDate());
                                existingRental.setEndDate(request.endDate());
                                existingRental.setRentalType(request.rentalType());
                                existingRental.setTotalAmount(breakdown.totalDue());
                                existingRental.setCommissionAmount(commission);
                                existingRental.setDepositAmount(breakdown.requestedUpfront());
                                existingRental.setRentalAmount(breakdown.rentalAmount());
                                existingRental.setCautionAmount(breakdown.cautionAmount());
                                existingRental.setRequestedUpfront(breakdown.requestedUpfront());
                                existingRental.setClientPhone(request.clientPhone());
                                existingRental.setClientName(clientLabel);
                                existingRental.setClientEmail(client.getEmail());
                                existingRental.setUpdatedAt(LocalDateTime.now());

                                return rentalRepository.save(existingRental);
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                // Création d'une nouvelle réservation si aucune n'existe
                                RentalEntity newRental = RentalEntity.builder()
                                    .id(UUID.randomUUID())
                                    .clientId(clientId)
                                    .clientName(clientLabel)
                                    .clientEmail(client.getEmail())
                                    .agencyId(vehicle.getAgencyId())
                                    .vehicleId(request.vehicleId())
                                    .driverId(request.driverId())
                                    .startDate(request.startDate())
                                    .endDate(request.endDate())
                                    .status(RentalStatus.PENDING)
                                    .rentalType(request.rentalType())
                                    .totalAmount(breakdown.totalDue())
                                    .amountPaid(BigDecimal.ZERO)
                                    .commissionAmount(commission)
                                    .depositAmount(breakdown.requestedUpfront())
                                    .rentalAmount(breakdown.rentalAmount())
                                    .cautionAmount(breakdown.cautionAmount())
                                    .rentalAmountPaid(BigDecimal.ZERO)
                                    .cautionAmountPaid(BigDecimal.ZERO)
                                    .cautionHeld(BigDecimal.ZERO)
                                    .requestedUpfront(breakdown.requestedUpfront())
                                    .trackedKm(BigDecimal.ZERO)
                                    .clientPhone(request.clientPhone())
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .isNewRecord(true)
                                    .build();
                                return rentalRepository.save(newRental);
                            }))
                            .map(saved -> new RentalInitResponse(
                                true,
                                String.format(NotificationTemplate.RESERVATION_INIT_CLIENT.getTemplate(),
                                    breakdown.requestedUpfront()),
                                saved.getId(), totalFinal, deposit, commission, agencyMapper.toDto(agency),
                                breakdown, loyaltyDiscount
                            ))
                            // Notification agence : nouvelle réservation reçue
                            .flatMap(response -> notificationService.createNotification(
                                response.rentalId(),
                                agency.getId(),
                                NotificationResourceType.AGENCY,
                                NotificationReason.RESERVATION_NEW,
                                request.vehicleId(),
                                request.driverId(),
                                NotificationTemplate.RESERVATION_INIT_AGENCY
                            ).thenReturn(response)));
                    });
                })));
    }

    // Création directe par l'agence (Walk-in) avec les nouveaux champs
    @Transactional
    public Mono<RentalInitResponse> createAgencyRental(UUID agencyId, AgencyRentalRequest request) {
        return validateRentalWindow(request.startDate(), request.endDate())
            .then(Mono.defer(() -> vehicleRepository.findById(request.vehicleId())))
            .filter(v -> v.getAgencyId().equals(agencyId))
            .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                    "Vehicle not found or does not belong to this agency")))
            .flatMap(vehicle -> Mono.zip(
                pricingService.getPricing(ResourceType.VEHICLE, request.vehicleId()),
                request.driverId() != null
                    ? pricingService.getPricing(ResourceType.DRIVER, request.driverId())
                    : Mono.just(new PricingEntity()),
                agencyRepository.findById(agencyId)
            ).flatMap(tuple -> {
                var vehiclePrice = tuple.getT1();
                var driverPrice = tuple.getT2();
                var agency = tuple.getT3();

                BigDecimal baseAmount = RentalDurationCalculator.computeBaseAmount(
                    request.startDate(), request.endDate(), request.rentalType(),
                    vehiclePrice, request.driverId() != null ? driverPrice : null);
                RentalPricingBreakdown breakdown = RentalPricingCalculator.computeBreakdown(
                    baseAmount, RentalConstants.PLATFORM_COMMISSION_RATE, agency.getDepositPercentage());
                BigDecimal commission = breakdown.commissionAmount();
                BigDecimal deposit = breakdown.requestedUpfront();
                BigDecimal totalFinal = breakdown.totalDue();

                return rentalRepository.countConflictingRentals(
                        request.vehicleId(), request.startDate(), request.endDate())
                    .flatMap(conflicts -> {
                        if (conflicts > 0) {
                            return Mono.error(new RentalConflictException(
                                "Vehicle is already booked for this period."));
                        }

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
                    .rentalAmount(breakdown.rentalAmount())
                    .cautionAmount(breakdown.cautionAmount())
                    .rentalAmountPaid(BigDecimal.ZERO)
                    .cautionAmountPaid(BigDecimal.ZERO)
                    .cautionHeld(BigDecimal.ZERO)
                    .requestedUpfront(breakdown.requestedUpfront())
                    .trackedKm(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isNewRecord(true)
                    .build();

                return rentalRepository.save(rental)
                    .flatMap(saved -> {
                        BigDecimal counterAmount = request.initialPaymentAmount() != null
                            ? request.initialPaymentAmount()
                            : breakdown.requestedUpfront();
                        PaymentMethod method = request.paymentMethod() != null
                            ? request.paymentMethod()
                            : PaymentMethod.CASH;
                        RentalInitResponse created = new RentalInitResponse(
                            true, "Réservation agence créée.",
                            saved.getId(), totalFinal, deposit, commission, agencyMapper.toDto(agency),
                            breakdown, java.math.BigDecimal.ZERO
                        );
                        if (counterAmount.compareTo(BigDecimal.ZERO) <= 0) {
                            return Mono.just(created);
                        }
                        return rentalPaymentUseCase.processPayment(
                                saved.getId(), new PaymentRequest(counterAmount, method))
                            .thenReturn(new RentalInitResponse(
                                true, "Réservation confirmée — acompte encaissé au comptoir.",
                                saved.getId(), totalFinal, deposit, commission, agencyMapper.toDto(agency),
                                breakdown, java.math.BigDecimal.ZERO
                            ));
                    });
                    });
            }));
    }

    /**
     * @deprecated R2 — remplacé par {@link #checkIn(UUID, CheckInRequest)} qui capture
     *     l'inspection CHECK_IN + le kilométrage de départ. Conservé pour rétro-compat.
     */
    @Deprecated
    @Transactional
    public Mono<RentalEntity> startRental(UUID rentalId) {
        log.warn("startRental(rentalId={}) is deprecated — use /check-in instead", rentalId);
        return rentalRepository.findById(rentalId)
            .filter(r -> r.getStatus() == RentalStatus.PAID)
            .switchIfEmpty(Mono.error(new RentalConflictException("Rental must be fully paid (PAID) before start.")))
            .flatMap(rental -> {
                rental.setStatus(RentalStatus.ONGOING);
                rental.setUpdatedAt(LocalDateTime.now());
                return rentalRepository.save(rental)
                    .flatMap(saved -> Mono.when(
                        saved.getClientId() != null ? notificationService.createNotification(
                            saved.getId(),
                            saved.getClientId(),
                            NotificationResourceType.CLIENT,
                            NotificationReason.LOCATION_START,
                            saved.getVehicleId(),
                            saved.getDriverId(),
                            NotificationTemplate.LOCATION_START_CLIENT
                        ) : Mono.empty(),
                        notificationService.createNotification(
                            saved.getId(),
                            saved.getAgencyId(),
                            NotificationResourceType.AGENCY,
                            NotificationReason.LOCATION_START,
                            saved.getVehicleId(),
                            saved.getDriverId(),
                            NotificationTemplate.LOCATION_START_AGENCY
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
                        saved.getId(),
                        saved.getAgencyId(),
                        NotificationResourceType.AGENCY,
                        NotificationReason.LOCATION_END_SIGNAL,
                        saved.getVehicleId(),
                        saved.getDriverId(),
                        NotificationTemplate.LOCATION_END_SIGNAL_AGENCY
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

                LocalDateTime maintenanceEnd = rental.getEndDate()
                    .plusHours(RentalConstants.MAINTENANCE_HOURS_AFTER_RETURN);
                ScheduleRequestDTO schedule = new ScheduleRequestDTO(
                    rental.getEndDate(), maintenanceEnd, "MAINTENANCE", "Révision post-location"
                );

                return Mono.when(
                    scheduleService.addUnavailability(
                            rental.getAgencyId(), ResourceType.VEHICLE, rental.getVehicleId(), schedule),
                    rental.getDriverId() != null
                        ? scheduleService.addUnavailability(
                            rental.getAgencyId(), ResourceType.DRIVER, rental.getDriverId(), schedule)
                        : Mono.empty()
                ).then(rentalRepository.save(rental))
                 .flatMap(saved -> Mono.when(
                     saved.getClientId() != null ? notificationService.createNotification(
                         saved.getId(),
                         saved.getClientId(),
                         NotificationResourceType.CLIENT,
                         NotificationReason.LOCATION_END,
                         saved.getVehicleId(),
                         saved.getDriverId(),
                         NotificationTemplate.LOCATION_END_VALIDATED_CLIENT
                     ) : Mono.empty(),
                     notificationService.createNotification(
                         saved.getId(),
                         saved.getAgencyId(),
                         NotificationResourceType.AGENCY,
                         NotificationReason.LOCATION_END,
                         saved.getVehicleId(),
                         saved.getDriverId(),
                         NotificationTemplate.LOCATION_END_VALIDATED_AGENCY
                     )
                 ).thenReturn(saved));
            });
    }

    // =====================================================================
    // R2 — Cycle location complet : check-in / signal-end / check-out / settle
    // =====================================================================

    @Override
    @Transactional
    public Mono<RentalDetailResponseDTO> checkIn(UUID rentalId, CheckInRequest request) {
        return rentalRepository.findById(rentalId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rental not found")))
            .flatMap(rental -> {
                if (rental.getStatus() != RentalStatus.PAID) {
                    return Mono.error(new ValidationException("MUST_BE_PAID"));
                }
                InspectionCreateRequest forced = forceInspectionType(request.inspection(), "CHECK_IN");
                return inspectionUseCase.createInspection(rentalId, forced)
                    .then(Mono.defer(() -> {
                        rental.setStartOdometer(request.startOdometer());
                        rental.setStatus(RentalStatus.ONGOING);
                        rental.setUpdatedAt(LocalDateTime.now());
                        return rentalRepository.save(rental);
                    }))
                    .flatMap(saved -> notifyClient(saved, NotificationReason.LOCATION_START,
                            NotificationTemplate.CHECK_IN_DONE_CLIENT).thenReturn(saved));
            })
            .flatMap(saved -> getRentalDetails(saved.getId()));
    }

    @Override
    @Transactional
    public Mono<RentalDetailResponseDTO> signalEnd(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rental not found")))
            .flatMap(rental -> {
                if (rental.getStatus() != RentalStatus.ONGOING) {
                    return Mono.error(new ValidationException("NOT_ONGOING"));
                }
                rental.setStatus(RentalStatus.UNDER_REVIEW);
                rental.setUpdatedAt(LocalDateTime.now());
                return rentalRepository.save(rental)
                    .flatMap(saved -> notificationService.createNotification(
                            saved.getId(), saved.getAgencyId(), NotificationResourceType.AGENCY,
                            NotificationReason.LOCATION_END_SIGNAL, saved.getVehicleId(), saved.getDriverId(),
                            NotificationTemplate.RETURN_UNDER_REVIEW_AGENCY)
                        .thenReturn(saved));
            })
            .flatMap(saved -> getRentalDetails(saved.getId()));
    }

    @Override
    @Transactional
    public Mono<RentalDetailResponseDTO> checkOut(UUID rentalId, CheckOutRequest request) {
        return rentalRepository.findById(rentalId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rental not found")))
            .flatMap(rental -> {
                if (rental.getStatus() != RentalStatus.UNDER_REVIEW) {
                    return Mono.error(new ValidationException("NOT_UNDER_REVIEW"));
                }
                InspectionCreateRequest forced = forceInspectionType(request.inspection(), "CHECK_OUT");
                return inspectionUseCase.createInspection(rentalId, forced)
                    .then(trackingUseCase.computeTrackedKm(rentalId).defaultIfEmpty(0.0))
                    .map(gpsKm -> {
                        rental.setEndOdometer(request.endOdometer());
                        double km = gpsKm;
                        if (km <= 0.0 && rental.getStartOdometer() != null && request.endOdometer() != null) {
                            km = request.endOdometer() - rental.getStartOdometer();
                        }
                        rental.setTrackedKm(BigDecimal.valueOf(Math.max(0.0, km))
                                .setScale(2, java.math.RoundingMode.HALF_UP));
                        rental.setUpdatedAt(LocalDateTime.now());
                        return rental;
                    })
                    .flatMap(rentalRepository::save);
            })
            .flatMap(saved -> getRentalDetails(saved.getId()));
    }

    @Override
    @Transactional
    public Mono<RentalDetailResponseDTO> settleReturn(UUID rentalId, CheckoutSettlementRequest request) {
        return rentalRepository.findById(rentalId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rental not found")))
            .flatMap(rental -> {
                if (rental.getStatus() != RentalStatus.UNDER_REVIEW) {
                    return Mono.error(new ValidationException("NOT_UNDER_REVIEW"));
                }
                BigDecimal damageCost = request.damageCost() == null
                        ? BigDecimal.ZERO : request.damageCost();
                BigDecimal held = rental.getCautionHeld() == null
                        ? BigDecimal.ZERO : rental.getCautionHeld();
                if (damageCost.signum() < 0) {
                    return Mono.error(new ValidationException("DAMAGE_COST_NEGATIVE"));
                }
                if (damageCost.signum() > 0
                        && (request.reason() == null || request.reason().isBlank())) {
                    return Mono.error(new ValidationException("REASON_REQUIRED"));
                }

                // Retenue = min(dommages, caution) ; remboursement = le reste ;
                // supplément dû (créance) = dommages au-delà de la caution.
                BigDecimal deduction = damageCost.min(held).setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal refunded = held.subtract(deduction).setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal supplement = damageCost.subtract(held).max(BigDecimal.ZERO)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                rental.setCautionDeducted(deduction);
                rental.setCautionRefunded(refunded);
                rental.setSupplementDue(supplement);
                rental.setStatus(RentalStatus.COMPLETED);
                rental.setUpdatedAt(LocalDateTime.now());

                Mono<Void> refundPayment = refunded.signum() > 0
                        ? paymentRepository.save(buildCautionPayment(
                                rentalId, refunded, "CAUTION_REFUND", null, BigDecimal.ZERO)).then()
                        : Mono.empty();
                // Retenue caution = revenu de l'agence → rental_portion = deduction.
                Mono<Void> retentionPayment = deduction.signum() > 0
                        ? paymentRepository.save(buildCautionPayment(
                                rentalId, deduction, "CAUTION_RETENTION", request.reason(), deduction)).then()
                        : Mono.empty();
                // Créance : supplément dû (non encore encaissé) — trace, rental_portion=0 tant qu'impayé.
                Mono<Void> supplementRecord = supplement.signum() > 0
                        ? paymentRepository.save(buildCautionPayment(
                                rentalId, supplement, "SUPPLEMENT_DUE", request.reason(), BigDecimal.ZERO)).then()
                        : Mono.empty();

                Mono<Void> escrowUpdate = agencyRepository.findById(rental.getAgencyId())
                        .flatMap(agency -> {
                            BigDecimal escrow = agency.getCautionEscrowBalance() == null
                                    ? BigDecimal.ZERO : agency.getCautionEscrowBalance();
                            agency.setCautionEscrowBalance(escrow.subtract(held));
                            // Retenue caution = entrée pour l'agence.
                            double rev = agency.getMonthlyRevenue() == null ? 0.0 : agency.getMonthlyRevenue();
                            agency.setMonthlyRevenue(rev + deduction.doubleValue());
                            return agencyRepository.save(agency);
                        }).then();

                Mono<Void> mileageUpdate = (rental.getStartOdometer() != null && rental.getEndOdometer() != null)
                        ? vehicleRepository.findById(rental.getVehicleId())
                            .flatMap(vehicle -> {
                                double base = vehicle.getKilometrage() == null ? 0.0 : vehicle.getKilometrage();
                                double delta = rental.getEndOdometer() - rental.getStartOdometer();
                                vehicle.setKilometrage(base + Math.max(0.0, delta));
                                return vehicleRepository.save(vehicle);
                            }).then()
                        : Mono.empty();

                Mono<Void> notifyAndEmail;
                if (deduction.signum() > 0) {
                    Mono<Void> inApp = rental.getClientId() != null
                            ? notificationService.createNotification(
                                    rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT,
                                    NotificationReason.CAUTION_DEDUCTION, rental.getVehicleId(), rental.getDriverId(),
                                    NotificationTemplate.CAUTION_DEDUCTION_APPLIED_CLIENT,
                                    deduction, request.reason(), refunded).then()
                            : Mono.empty();
                    Mono<Void> email = rentalEmailPort.sendCautionDeduction(
                            rental.getClientEmail(), deduction, request.reason(), refunded);
                    notifyAndEmail = inApp.then(email);
                } else {
                    Mono<Void> inApp = rental.getClientId() != null
                            ? notificationService.createNotification(
                                    rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT,
                                    NotificationReason.REFUND_PROCESSED, rental.getVehicleId(), rental.getDriverId(),
                                    NotificationTemplate.CAUTION_FULLY_REFUNDED_CLIENT, refunded).then()
                            : Mono.empty();
                    Mono<Void> email = rentalEmailPort.sendCautionFullyRefunded(rental.getClientEmail(), refunded);
                    notifyAndEmail = inApp.then(email);
                }

                // Notification de dette si les dommages dépassent la caution.
                Mono<Void> debtNotify = (supplement.signum() > 0 && rental.getClientId() != null)
                        ? notificationService.createNotification(
                                rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT,
                                NotificationReason.CAUTION_DEDUCTION, rental.getVehicleId(), rental.getDriverId(),
                                NotificationTemplate.CLIENT_DEBT_CREATED, supplement).then()
                        : Mono.empty();

                // R3 : gain de points de fidélité sur la part location encaissée (COMPLETED).
                Mono<Void> earnLoyalty = rental.getClientId() != null
                        ? loyaltyUseCase.earnFromRental(rental.getClientId(),
                                rental.getRentalAmountPaid() == null ? BigDecimal.ZERO : rental.getRentalAmountPaid(),
                                rental.getId()).then()
                        : Mono.empty();

                return rentalRepository.save(rental)
                        .then(refundPayment)
                        .then(retentionPayment)
                        .then(supplementRecord)
                        .then(escrowUpdate)
                        .then(mileageUpdate)
                        .then(notifyAndEmail)
                        .then(debtNotify)
                        .then(earnLoyalty)
                        .thenReturn(rental);
            })
            .flatMap(saved -> getRentalDetails(saved.getId()));
    }

    @Override
    @Transactional
    public Mono<RentalDetailResponseDTO> collectSupplement(UUID rentalId, BigDecimal amount) {
        return rentalRepository.findById(rentalId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rental not found")))
            .flatMap(rental -> {
                BigDecimal due = rental.getSupplementDue() == null ? BigDecimal.ZERO : rental.getSupplementDue();
                BigDecimal amt = amount == null ? BigDecimal.ZERO : amount;
                if (amt.signum() <= 0) {
                    return Mono.error(new ValidationException("AMOUNT_REQUIRED"));
                }
                if (amt.compareTo(due) > 0) {
                    return Mono.error(new ValidationException("EXCEEDS_SUPPLEMENT_DUE"));
                }
                rental.setSupplementDue(due.subtract(amt).setScale(2, java.math.RoundingMode.HALF_UP));
                rental.setUpdatedAt(LocalDateTime.now());
                // Encaissement du supplément = revenu agence (rental_portion = montant).
                Mono<Void> payment = paymentRepository.save(buildCautionPayment(
                        rentalId, amt, "SUPPLEMENT_PAID", null, amt)).then();
                Mono<Void> revenue = agencyRepository.findById(rental.getAgencyId())
                        .flatMap(agency -> {
                            double rev = agency.getMonthlyRevenue() == null ? 0.0 : agency.getMonthlyRevenue();
                            agency.setMonthlyRevenue(rev + amt.doubleValue());
                            return agencyRepository.save(agency);
                        }).then();
                return rentalRepository.save(rental).then(payment).then(revenue).thenReturn(rental);
            })
            .flatMap(saved -> getRentalDetails(saved.getId()));
    }

    @Override
    public Mono<BigDecimal> getClientDebtForAgency(UUID clientId, UUID agencyId) {
        return agencyRepository.findById(agencyId)
            .flatMap(agency -> agency.getOrganizationId() == null
                ? Mono.just(BigDecimal.ZERO)
                : rentalRepository.findClientDebtRentals(clientId, agency.getOrganizationId())
                    .map(r -> r.getSupplementDue() == null ? BigDecimal.ZERO : r.getSupplementDue())
                    .reduce(BigDecimal.ZERO, BigDecimal::add))
            .defaultIfEmpty(BigDecimal.ZERO);
    }

    @Override
    public Flux<RentalEntity> getOrganizationDebts(UUID orgId) {
        return rentalRepository.findOrganizationDebts(orgId);
    }

    @Override
    public Flux<RentalEntity> getAgencyDebts(UUID agencyId) {
        return rentalRepository.findAgencyDebts(agencyId);
    }

    @Override
    public Mono<BigDecimal> getTotalOutstandingDebt() {
        return rentalRepository.sumOutstandingDebt().defaultIfEmpty(BigDecimal.ZERO);
    }

    private Mono<Void> notifyClient(RentalEntity rental, NotificationReason reason, NotificationTemplate template) {
        if (rental.getClientId() == null) {
            return Mono.empty();
        }
        return notificationService.createNotification(
                rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT,
                reason, rental.getVehicleId(), rental.getDriverId(), template).then();
    }

    private InspectionCreateRequest forceInspectionType(InspectionCreateRequest src, String type) {
        if (src == null) {
            return new InspectionCreateRequest(type, null, null, null, null, null);
        }
        return new InspectionCreateRequest(
                type, src.odometer(), src.fuelLevel(), src.notes(), src.photoUrls(), src.items());
    }

    private PaymentEntity buildCautionPayment(UUID rentalId, BigDecimal amount, String category,
            String reason, BigDecimal rentalPortion) {
        return PaymentEntity.builder()
                .id(UUID.randomUUID())
                .rentalId(rentalId)
                .amount(amount)
                .paymentMethod(PaymentMethod.CASH)
                .transactionDate(LocalDateTime.now())
                .transactionRef(reason)
                .paymentCategory(category)
                .rentalPortion(rentalPortion)
                .cautionPortion(amount.subtract(rentalPortion))
                .isNewRecord(true)
                .build();
    }

    @Transactional
    public Mono<RentalEntity> cancelRental(UUID rentalId) {
        return rentalRepository.findById(rentalId)
            .filter(r -> r.getStatus() == RentalStatus.RESERVED
                    || r.getStatus() == RentalStatus.PAID
                    || r.getStatus() == RentalStatus.PENDING)
            .switchIfEmpty(Mono.error(new RentalConflictException("Cannot cancel this reservation.")))
            .flatMap(rental -> {
                BigDecimal amountPaid = rental.getAmountPaid();
                BigDecimal penalty = amountPaid.multiply(RentalConstants.PENALTY_RATE);
                BigDecimal refundAmount = amountPaid.subtract(penalty);

                rental.setStatus(RentalStatus.CANCELLED);
                rental.setUpdatedAt(LocalDateTime.now());

                Mono<Void> freeSchedule = scheduleService.removeScheduleForRental(
                        rental.getVehicleId(), rental.getDriverId(), rental.getStartDate(), rental.getEndDate());

                return rentalRepository.save(rental)
                    .flatMap(saved -> freeSchedule
                        .then(Mono.when(
                            saved.getClientId() != null ? notificationService.createNotification(
                                saved.getId(),
                                saved.getClientId(),
                                NotificationResourceType.CLIENT,
                                NotificationReason.CANCELLATION,
                                saved.getVehicleId(),
                                saved.getDriverId(),
                                NotificationTemplate.CANCELLATION_CLIENT,
                                amountPaid, penalty, refundAmount
                            ) : Mono.empty(),
                            notificationService.createNotification(
                                saved.getId(),
                                saved.getAgencyId(),
                                NotificationResourceType.AGENCY,
                                NotificationReason.CANCELLATION,
                                saved.getVehicleId(),
                                saved.getDriverId(),
                                NotificationTemplate.CANCELLATION_AGENCY,
                                penalty
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

    /**
     * Rejects bookings whose start is already in the past, or whose end is not after start.
     * Allows a small grace window so clock skew / form submit latency does not block "now".
     */
    private Mono<Void> validateRentalWindow(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return Mono.error(new ValidationException("Start date and end date are required"));
        }
        if (!endDate.isAfter(startDate)) {
            return Mono.error(new ValidationException("End date must be after start date"));
        }
        LocalDateTime earliestAllowed = LocalDateTime.now().minusMinutes(5);
        if (startDate.isBefore(earliestAllowed)) {
            return Mono.error(new ValidationException(
                    "Cannot create a reservation with a start date in the past"));
        }
        return Mono.empty();
    }
}
