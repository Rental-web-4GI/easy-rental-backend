package com.yowyob.easyrental.modules.statistics.dto;

import java.util.List;

// Objet complet renvoyé au Frontend
public record FullDashboardDTO(
    GlobalStatsDTO summary,
    TimeSeriesDataDTO revenueEvolution, // Graphe évolution revenus
    TimeSeriesDataDTO rentalEvolution,  // Graphe évolution nombre locations
    DistributionDataDTO vehicleStatusDistribution, // Pie chart état véhicules
    DistributionDataDTO rentalStatusDistribution,  // Pie chart état locations
    List<AgencyComparisonDTO> agencyComparison // Tableau comparatif (si Organisation)
) {}
