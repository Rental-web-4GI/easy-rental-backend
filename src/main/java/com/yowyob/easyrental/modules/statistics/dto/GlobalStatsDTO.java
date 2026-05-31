package com.yowyob.easyrental.modules.statistics.dto;

import java.math.BigDecimal;

// Résumé numérique (Cartes en haut du dashboard)
public record GlobalStatsDTO(
    Long totalAgencies,
    Long totalVehicles,
    Long totalDrivers,
    Long totalStaff,
    Long totalRentals,
    Long activeRentals,
    Long totalReservations,
    BigDecimal totalRevenue,
    BigDecimal monthlyRevenue
) {}
