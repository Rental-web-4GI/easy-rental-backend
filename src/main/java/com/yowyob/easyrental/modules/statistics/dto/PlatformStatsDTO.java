package com.yowyob.easyrental.modules.statistics.dto;

import java.math.BigDecimal;

/**
 * Statistiques globales de la plateforme, consommées par le dashboard admin.
 *
 * @author Easy Rental Team
 * @since 2026-07-24
 */
public record PlatformStatsDTO(
        UserCounts users,
        OrgCounts organizations,
        AgencyCounts agencies,
        VehicleCounts vehicles,
        RentalCounts rentals,
        RevenueSummary revenue
) {

    public record UserCounts(
            long total,
            long clients,
            long orgOwners,
            long freelances,
            long staff
    ) {}

    public record OrgCounts(
            long total,
            long companies,
            long freelances,
            long suspended
    ) {}

    public record AgencyCounts(
            long total,
            double averagePerCompany
    ) {}

    public record VehicleCounts(
            long total,
            long published
    ) {}

    public record RentalCounts(
            long total,
            long ongoing,
            long completed,
            long monthlyCompleted
    ) {}

    public record RevenueSummary(
            BigDecimal subscriptionsMonthlyMRR,
            long subscriptionsActiveCount
    ) {}
}
