package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.notification.domain.NotificationTemplate;
import com.yowyob.easyrental.modules.notification.domain.port.in.NotificationUseCase;
import com.yowyob.easyrental.modules.rental.domain.PaymentEntity;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.dto.PaymentRequest;
import com.yowyob.easyrental.modules.rental.domain.port.out.PaymentRepositoryPort;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import com.yowyob.easyrental.shared.dto.ScheduleRequestDTO;
import com.yowyob.easyrental.modules.rental.domain.port.in.RentalPaymentUseCase;
import com.yowyob.easyrental.shared.constants.RentalConstants;
import com.yowyob.easyrental.shared.exception.ResourceNotFoundException;
import com.yowyob.easyrental.shared.enums.NotificationReason;
import com.yowyob.easyrental.shared.enums.NotificationResourceType;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import com.yowyob.easyrental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalPaymentUseCaseImpl implements RentalPaymentUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final RentalRepositoryPort rentalRepository;
    private final AgencyRepositoryPort agencyRepository;
    private final ScheduleUseCase scheduleService;
    private final NotificationUseCase notificationService;

    @Transactional
    public Mono<RentalEntity> processPayment(UUID rentalId, PaymentRequest request) {
        return rentalRepository.findById(rentalId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Rental not found")))
            .flatMap(rental -> {
                BigDecimal amount = request.amount();

                // R2 proportional allocation: split the incoming amount between the rental fee
                // (agency revenue) and the caution (agency escrow), pro-rata to rentalAmount/cautionAmount.
                // Legacy rentals (created before the R2 pricing breakdown) have no rentalAmount/
                // cautionAmount set: fall back to totalAmount as 100% rental fee, 0% caution.
                BigDecimal rentalAmount = rental.getRentalAmount();
                BigDecimal cautionAmount = rental.getCautionAmount();
                BigDecimal totalDue;
                if (rentalAmount == null) {
                    rentalAmount = rental.getTotalAmount() == null ? BigDecimal.ZERO : rental.getTotalAmount();
                    cautionAmount = BigDecimal.ZERO;
                    totalDue = rentalAmount;
                } else {
                    cautionAmount = cautionAmount == null ? BigDecimal.ZERO : cautionAmount;
                    totalDue = rentalAmount.add(cautionAmount);
                }

                BigDecimal rentalPortion;
                BigDecimal cautionPortion;
                if (totalDue.compareTo(BigDecimal.ZERO) <= 0) {
                    // Nothing to prorate against: treat as pure rental fee (no caution due).
                    rentalPortion = amount;
                    cautionPortion = BigDecimal.ZERO;
                } else {
                    // Single division (amount * rentalAmount / totalDue) rather than pre-rounding a
                    // ratio to a fixed scale and then multiplying — avoids compounding rounding error.
                    rentalPortion = amount.multiply(rentalAmount)
                        .divide(totalDue, 2, RoundingMode.HALF_UP);
                    // cautionPortion via subtraction guarantees rentalPortion + cautionPortion == amount.
                    cautionPortion = amount.subtract(rentalPortion);
                }

                // paymentCategory convention: RENTAL_FEE when the payment is (or falls back to being)
                // pure rental fee, CAUTION when it is pure caution, MIXED otherwise (the R2 norm for an
                // initial 60% upfront payment, which always covers part of both).
                String paymentCategory;
                if (cautionPortion.compareTo(BigDecimal.ZERO) == 0) {
                    paymentCategory = "RENTAL_FEE";
                } else if (rentalPortion.compareTo(BigDecimal.ZERO) == 0) {
                    paymentCategory = "CAUTION";
                } else {
                    paymentCategory = "MIXED";
                }

                // 1. Enregistrement du paiement
                PaymentEntity payment = PaymentEntity.builder()
                    .id(UUID.randomUUID())
                    .rentalId(rentalId)
                    .amount(amount)
                    .paymentMethod(request.method())
                    .transactionDate(LocalDateTime.now())
                    .transactionRef("TXN-" + System.currentTimeMillis())
                    .paymentCategory(paymentCategory)
                    .rentalPortion(rentalPortion)
                    .cautionPortion(cautionPortion)
                    .isNewRecord(true)
                    .build();

                BigDecimal finalRentalPortion = rentalPortion;
                BigDecimal finalCautionPortion = cautionPortion;
                BigDecimal finalTotalDue = totalDue;

                return paymentRepository.save(payment).flatMap(savedPayment -> {
                    // 2. Mise à jour du montant payé (legacy mirror + ventilation R2)
                    BigDecimal previousAmountPaid = rental.getAmountPaid() == null
                        ? BigDecimal.ZERO : rental.getAmountPaid();
                    BigDecimal newAmountPaid = previousAmountPaid.add(amount);
                    rental.setAmountPaid(newAmountPaid);

                    BigDecimal newRentalAmountPaid = (rental.getRentalAmountPaid() == null
                        ? BigDecimal.ZERO : rental.getRentalAmountPaid()).add(finalRentalPortion);
                    BigDecimal newCautionAmountPaid = (rental.getCautionAmountPaid() == null
                        ? BigDecimal.ZERO : rental.getCautionAmountPaid()).add(finalCautionPortion);
                    rental.setRentalAmountPaid(newRentalAmountPaid);
                    rental.setCautionAmountPaid(newCautionAmountPaid);
                    // Mirror until checkout settlement (Task 7) applies deductions/refunds.
                    rental.setCautionHeld(newCautionAmountPaid);

                    // 3. Logique des seuils (60% = RESERVED, 100% = PAID), jamais de régression de statut
                    BigDecimal totalPaid = newRentalAmountPaid.add(newCautionAmountPaid);
                    BigDecimal sixtyPercent = finalTotalDue.multiply(RentalConstants.RESERVATION_DEPOSIT_RATE);

                    RentalStatus oldStatus = rental.getStatus();
                    RentalStatus computedStatus;
                    if (totalPaid.compareTo(finalTotalDue) >= 0) {
                        computedStatus = RentalStatus.PAID;
                    } else if (totalPaid.compareTo(sixtyPercent) >= 0) {
                        computedStatus = RentalStatus.RESERVED;
                    } else {
                        computedStatus = RentalStatus.PENDING;
                    }
                    RentalStatus newStatus = statusRank(computedStatus) > statusRank(oldStatus)
                        ? computedStatus : oldStatus;

                    rental.setStatus(newStatus);

                    // 4. Mise à jour revenus Agence (rentalPortion uniquement — jamais amount) + escrow caution
                    Mono<Void> updateRevenue = agencyRepository.findById(rental.getAgencyId())
                        .flatMap(agency -> {
                            double currentRevenue = agency.getMonthlyRevenue() == null
                                ? 0.0 : agency.getMonthlyRevenue();
                            agency.setMonthlyRevenue(currentRevenue + finalRentalPortion.doubleValue());
                            BigDecimal currentEscrow = agency.getCautionEscrowBalance() == null
                                ? BigDecimal.ZERO : agency.getCautionEscrowBalance();
                            agency.setCautionEscrowBalance(currentEscrow.add(finalCautionPortion));
                            return agencyRepository.save(agency);
                        }).then();

                    // 5. Blocage Planning (Si passage à RESERVED ou PAID pour la première fois)
                    Mono<Void> blockSchedule = Mono.empty();
                    if (oldStatus == RentalStatus.PENDING
                            && (newStatus == RentalStatus.RESERVED || newStatus == RentalStatus.PAID)) {
                        ScheduleRequestDTO schedule = new ScheduleRequestDTO(
                            rental.getStartDate(), rental.getEndDate(), "RENTED", "Location #" + rental.getId()
                        );
                        Mono<Void> blockVehicle = scheduleService.addUnavailability(
                            rental.getAgencyId(), ResourceType.VEHICLE, rental.getVehicleId(), schedule)
                            .then();
                        Mono<Void> blockDriver = rental.getDriverId() != null
                            ? scheduleService.addUnavailability(
                                rental.getAgencyId(), ResourceType.DRIVER, rental.getDriverId(), schedule)
                                .then()
                            : Mono.empty();
                        blockSchedule = Mono.when(blockVehicle, blockDriver);
                    }

                    // 6. Notifications (Utilisation des Templates)
                    Mono<Void> notifyClient = (rental.getClientId() != null) ? notificationService.createNotification(
                        rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT,
                                NotificationReason.PAYMENT_RECEIVED,
                        rental.getVehicleId(), rental.getDriverId(),
                        NotificationTemplate.PAYMENT_RECEIVED_CLIENT, request.amount(), newAmountPaid,
                        finalTotalDue, newStatus
                    ).then() : Mono.empty();

                    Mono<Void> notifyAgency = notificationService.createNotification(
                        rental.getId(), rental.getAgencyId(), NotificationResourceType.AGENCY,
                                NotificationReason.PAYMENT_RECEIVED,
                        rental.getVehicleId(), rental.getDriverId(),
                        NotificationTemplate.PAYMENT_RECEIVED_AGENCY, request.amount(), rental.getId()
                    ).then();

                    // Notification spécifique "Réservation Réussie" (Passage à RESERVED)
                    Mono<Void> notifyReservationSuccess = Mono.empty();
                    if (oldStatus == RentalStatus.PENDING && newStatus == RentalStatus.RESERVED) {
                        Mono<Void> notifyClientReserved = rental.getClientId() != null
                            ? notificationService.createNotification(
                                rental.getId(), rental.getClientId(), NotificationResourceType.CLIENT,
                                NotificationReason.RESERVATION_CREATED,
                                rental.getVehicleId(), rental.getDriverId(),
                                NotificationTemplate.RESERVATION_CONFIRMED_CLIENT, rental.getId()
                            ).then()
                            : Mono.empty();
                        Mono<Void> notifyAgencyReserved = notificationService.createNotification(
                            rental.getId(), rental.getAgencyId(), NotificationResourceType.AGENCY,
                            NotificationReason.RESERVATION_CREATED,
                            rental.getVehicleId(), rental.getDriverId(),
                            NotificationTemplate.RESERVATION_CONFIRMED_AGENCY, rental.getId(),
                            RentalClientLabelResolver.resolve(rental)
                        ).then();
                        Mono<Void> notifyDriverReserved = rental.getDriverId() != null
                            ? notificationService.createNotification(
                                rental.getId(), rental.getDriverId(), NotificationResourceType.DRIVER,
                                NotificationReason.RESERVATION_CREATED,
                                rental.getVehicleId(), rental.getDriverId(),
                                NotificationTemplate.RESERVATION_CONFIRMED_DRIVER, rental.getStartDate(),
                                rental.getEndDate()
                            ).then()
                            : Mono.empty();
                        notifyReservationSuccess = Mono.when(
                            notifyClientReserved, notifyAgencyReserved, notifyDriverReserved);
                    }

                    return updateRevenue
                        .then(blockSchedule)
                        .then(notifyClient)
                        .then(notifyAgency)
                        .then(notifyReservationSuccess)
                        .then(rentalRepository.save(rental));
                });
            });
    }

    /**
     * Monotonic rank used to ensure payment-driven status transitions never regress:
     * PENDING &lt; RESERVED &lt; PAID. Any later lifecycle status (ONGOING, UNDER_REVIEW, COMPLETED,
     * CANCELLED) already sits beyond the payment phase and must never be pulled back down here.
     */
    private static int statusRank(RentalStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case RESERVED -> 1;
            case PAID -> 2;
            default -> 3;
        };
    }
}
