package com.yowyob.easyrental.shared.enums;

public enum RentalStatus {
    PENDING,        // Créé, en attente de paiement initial
    RESERVED,       // Acompte (60%) payé - Réservation confirmée
    PAID,           // Totalité (100%) payée - Location confirmée
    ONGOING,        // Véhicule récupéré, location en cours
    UNDER_REVIEW,   // Véhicule retourné, en attente de validation agence
    COMPLETED,      // Clôturé (Maintenance post-location déclenchée)
    CANCELLED       // Annulé par le client ou l'agence
}
