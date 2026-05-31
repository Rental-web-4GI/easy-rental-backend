package com.yowyob.easyrental.modules.vehicle.dto;

/**
 * DTO pour les détails de consommation de carburant.
 * Correspond à l'objet fuel_efficiency du diagramme.
 */
public record FuelEfficiencyDTO(
    String city,    // Consommation en ville
    String highway  // Consommation sur autoroute
) {}