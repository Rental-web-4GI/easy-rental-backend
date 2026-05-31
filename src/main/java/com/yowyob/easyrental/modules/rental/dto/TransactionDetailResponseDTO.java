package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.shared.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionDetailResponseDTO(
    UUID id,
    String type,            // "RENTAL_PAYMENT" ou "SUBSCRIPTION_COST"
    BigDecimal amount,
    String description,
    LocalDateTime date,
    String reference,
    String status,
    PaymentMethod method,   // Null pour les abonnements
    RentalDetailResponseDTO rentalDetails, // Rempli si c'est un paiement de location
    SubscriptionPlanEntity planDetails     // Rempli si c'est un paiement d'abonnement
) {}
