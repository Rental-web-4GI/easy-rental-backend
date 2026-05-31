package com.yowyob.easyrental.modules.notification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationTemplate {

    // --- RÉSERVATION ---
    RESERVATION_INIT_CLIENT("Réservation initiée. Veuillez payer 60%% du montant (%s XAF) pour confirmer."),
    RESERVATION_INIT_AGENCY("Nouvelle demande de réservation reçue (Client App)."),

    RESERVATION_CONFIRMED_CLIENT("Réservation confirmée ! Code: %s. Le véhicule est réservé."),
    RESERVATION_CONFIRMED_AGENCY("Réservation confirmée (Code: %s) pour le client %s."),
    RESERVATION_CONFIRMED_DRIVER("Nouvelle course confirmée du %s au %s."),

    // --- PAIEMENT ---
    PAYMENT_RECEIVED_CLIENT("Paiement de %s XAF reçu. Total payé: %s / %s. Statut: %s."),
    PAYMENT_RECEIVED_AGENCY("Paiement reçu (%s XAF) pour la location #%s."),

    // --- DÉROULEMENT LOCATION ---
    LOCATION_START_CLIENT("Votre location a commencé. Bonne route !"),
    LOCATION_START_AGENCY("Le véhicule est sorti. Location démarrée."),

    LOCATION_END_SIGNAL_AGENCY("Le client a signalé le retour du véhicule. Veuillez valider l'état."),

    LOCATION_END_VALIDATED_CLIENT("Retour validé. Merci pour votre confiance. À bientôt !"),
    LOCATION_END_VALIDATED_AGENCY("Retour validé. Véhicule mis en maintenance pour 24h."),

    // --- ANNULATION ---
    CANCELLATION_CLIENT("Annulation confirmée. Montant payé: %s XAF. Pénalité (5%%): %s XAF. Remboursement: %s XAF."),
    CANCELLATION_AGENCY("Réservation annulée par le client. Pénalité retenue: %s XAF.");

    private final String template;

    /**
     * Formate le message avec les arguments fournis
     */
    public String format(Object... args) {
        return String.format(template, args);
    }
}
