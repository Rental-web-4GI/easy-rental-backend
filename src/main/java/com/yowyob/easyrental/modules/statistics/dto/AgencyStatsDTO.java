package com.yowyob.easyrental.modules.statistics.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AgencyStatsDTO(
    String agencyName,
    BigDecimal totalRevenue,
    BigDecimal monthlyRevenue, // Revenu du mois courant ou spécifié
    BigDecimal yearlyRevenue,  // Revenu de l'année courante ou spécifiée
    Long totalRentals,
    Long activeRentals,
    Long completedRentals,
    Long cancelledRentals,
    Map<String, Long> vehicleUsageStats, // Ex: "Toyota Corolla" -> 15 locations
    Map<String, Long> driverActivityStats // Ex: "Jean Dupont" -> 12 missions
) {}
