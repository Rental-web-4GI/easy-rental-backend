package com.yowyob.easyrental.modules.statistics.dto;

import java.util.Map;

// Pour les graphiques camemberts (Pie Chart) (ex: Statut des véhicules)
public record DistributionDataDTO(
    Map<String, Long> distribution // ex: {"AVAILABLE": 10, "RENTED": 5, "MAINTENANCE": 2}
) {}
