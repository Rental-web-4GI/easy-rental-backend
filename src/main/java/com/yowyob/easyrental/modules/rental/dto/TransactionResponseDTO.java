package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.shared.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDTO(
    UUID id,
    String type,            // "RENTAL_PAYMENT" ou "SUBSCRIPTION_PAYMENT"
    BigDecimal amount,
    String description,     // Ex: "Location Toyota - Client X" ou "Abonnement PRO"
    LocalDateTime date,
    String reference,       // Ref transaction ou ID
    String status,          // COMPLETED (par défaut pour l'historique)
    PaymentMethod method    // MOMO, CARD, etc. (Null pour les souscriptions si non stocké)
) {}
