package com.yowyob.easyrental.modules.subscription.domain;

/**
 * Énumération des types de plans de souscription disponibles.
 * Utilisée pour la validation des requêtes d'upgrade et la logique métier.
 */
public enum PlanType {
    FREE,
    PRO,
    PRO_YEARLY,
    ENTERPRISE,
    ENTERPRISE_YEARLY
}
