package com.project.apirental.shared.enums;

public enum NotificationReason {
    RESERVATION_NEW,         // Nouvelle demande de réservation reçue (PENDING)
    RESERVATION_CREATED,    // Succès de la réservation (60% payé)
    PAYMENT_COMPLETED,      // 100% payé
    LOCATION_START,         // Début effectif
    LOCATION_END_SIGNAL,    // Signalement retour
    LOCATION_END,           // Validation retour
    CANCELLATION,           // Annulation
    REFUND_PROCESSED,       // Remboursement
    PAYMENT_RECEIVED,       // Paiement reçu
    MAINTENANCE_SCHEDULED   // Maintenance auto
}
