package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.shared.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDTO(
    UUID id,
    String type,            // "RENTAL_PAYMENT" ou "SUBSCRIPTION_PAYMENT"
    BigDecimal amount,      // montant encaissé total du mouvement
    String description,     // Ex: "Location Toyota - Client X" ou "Abonnement PRO"
    LocalDateTime date,
    String reference,       // Ref transaction ou ID
    String status,          // COMPLETED (par défaut pour l'historique)
    PaymentMethod method,   // MOMO, CARD, etc.
    // Ventilation R2 : le front distingue revenu réel vs caution (escrow) vs remboursement.
    String category,        // RENTAL_FEE, CAUTION, CAUTION_REFUND, CAUTION_RETENTION, SUPPLEMENT_DUE, SUPPLEMENT_PAID
    BigDecimal rentalPortion,   // part réellement acquise à l'agence (revenu)
    BigDecimal cautionPortion   // part caution (escrow, non-revenu tant que non retenue)
) {}
