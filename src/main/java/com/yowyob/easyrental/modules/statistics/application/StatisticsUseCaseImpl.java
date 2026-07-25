package com.yowyob.easyrental.modules.statistics.application;

import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.modules.statistics.dto.AgencyComparisonDTO;
import com.yowyob.easyrental.modules.statistics.dto.AgencyStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.DistributionDataDTO;
import com.yowyob.easyrental.modules.statistics.dto.FullDashboardDTO;
import com.yowyob.easyrental.modules.statistics.dto.GlobalStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.OrgStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.PlatformStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.TimeSeriesDataDTO;
import com.yowyob.easyrental.modules.statistics.domain.port.in.StatisticsUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatisticsUseCaseImpl implements StatisticsUseCase {

    private final DatabaseClient databaseClient;
    private final AgencyRepositoryPort agencyRepository;
    private final UserRepositoryPort userRepository;
    private final OrganizationRepositoryPort organizationRepository;
    private final VehicleRepositoryPort vehicleRepository;
    private final RentalRepositoryPort rentalRepository;
    private final SubscriptionRepositoryPort subscriptionRepository;

    public Mono<FullDashboardDTO> getAgencyDashboard(UUID agencyId, int year) {
        return Mono.zip(
            getGlobalStats(agencyId),
            getRevenueEvolution(agencyId, year),
            getRentalEvolution(agencyId, year),
            getVehicleStatusDistribution(agencyId),
            getRentalStatusDistribution(agencyId)
        ).map(tuple -> new FullDashboardDTO(
            tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4(), tuple.getT5(), List.of()
        ));
    }

    public Mono<FullDashboardDTO> getOrganizationDashboard(UUID orgId, int year) {
        return agencyRepository.findAllByOrganizationId(orgId)
            .map(agency -> agency.getId())
            .collectList()
            .flatMap(agencyIds -> {
                if (agencyIds.isEmpty()) {
                    return Mono.just(new FullDashboardDTO(
                        new GlobalStatsDTO(0L, 0L, 0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO),
                        new TimeSeriesDataDTO(List.of(), List.of()),
                        new TimeSeriesDataDTO(List.of(), List.of()),
                        new DistributionDataDTO(Map.of()),
                        new DistributionDataDTO(Map.of()),
                        List.of()
                    ));
                }

                return Mono.zip(
                    getGlobalStatsForOrg(orgId),
                    getRevenueEvolutionForOrg(agencyIds, year),
                    getRentalEvolutionForOrg(agencyIds, year),
                    getVehicleStatusDistributionForOrg(agencyIds),
                    getRentalStatusDistributionForOrg(agencyIds),
                    getAgencyComparison(orgId)
                ).map(tuple -> new FullDashboardDTO(
                    tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4(), tuple.getT5(), tuple.getT6()
                ));
            });
    }

    // ==========================================
    // MÉTHODES POUR AGENCE UNIQUE
    // ==========================================

    private Mono<GlobalStatsDTO> getGlobalStats(UUID agencyId) {
        String sql = """
            SELECT
                (SELECT COUNT(*) FROM vehicles WHERE agency_id = :id) as vehicles,
                (SELECT COUNT(*) FROM drivers WHERE agency_id = :id) as drivers,
                (SELECT COUNT(*) FROM users WHERE agency_id = :id AND role = 'STAFF') as staff,
                (SELECT COUNT(*) FROM rentals WHERE agency_id = :id) as total_rentals,
                (SELECT COUNT(*) FROM rentals WHERE agency_id = :id AND status = 'ONGOING') as active_rentals,
                (SELECT COUNT(*) FROM rentals WHERE agency_id = :id AND status = 'RESERVED') as reservations,
                (SELECT COALESCE(SUM(COALESCE(p.rental_portion, p.amount)), 0)
                    FROM payments p JOIN rentals r ON p.rental_id = r.id
                    WHERE r.agency_id = :id) as total_rev,
                (SELECT COALESCE(SUM(COALESCE(p.rental_portion, p.amount)), 0)
                    FROM payments p JOIN rentals r ON p.rental_id = r.id
                    WHERE r.agency_id = :id
                    AND EXTRACT(MONTH FROM p.transaction_date) = EXTRACT(MONTH FROM CURRENT_DATE)) as month_rev
        """;

        return databaseClient.sql(sql)
            .bind("id", agencyId)
            .map(row -> new GlobalStatsDTO(
                1L,
                row.get("vehicles", Long.class),
                row.get("drivers", Long.class),
                row.get("staff", Long.class),
                row.get("total_rentals", Long.class),
                row.get("active_rentals", Long.class),
                row.get("reservations", Long.class),
                row.get("total_rev", BigDecimal.class),
                row.get("month_rev", BigDecimal.class)
            )).one();
    }

    private Mono<TimeSeriesDataDTO> getRevenueEvolution(UUID agencyId, int year) {
        String sql = """
            SELECT TO_CHAR(transaction_date, 'Mon') as month,
                   SUM(COALESCE(p.rental_portion, p.amount)) as total
            FROM payments p JOIN rentals r ON p.rental_id = r.id
            WHERE r.agency_id = :id AND EXTRACT(YEAR FROM transaction_date) = :year
            GROUP BY EXTRACT(MONTH FROM transaction_date), TO_CHAR(transaction_date, 'Mon')
            ORDER BY EXTRACT(MONTH FROM transaction_date)
        """;
        return buildTimeSeries(sql, agencyId, year);
    }

    private Mono<TimeSeriesDataDTO> getRentalEvolution(UUID agencyId, int year) {
        String sql = """
            SELECT TO_CHAR(created_at, 'Mon') as month, COUNT(*) as total
            FROM rentals
            WHERE agency_id = :id AND EXTRACT(YEAR FROM created_at) = :year
            GROUP BY EXTRACT(MONTH FROM created_at), TO_CHAR(created_at, 'Mon')
            ORDER BY EXTRACT(MONTH FROM created_at)
        """;
        return buildTimeSeries(sql, agencyId, year);
    }

    private Mono<DistributionDataDTO> getVehicleStatusDistribution(UUID agencyId) {
        return databaseClient.sql(
                "SELECT statut, COUNT(*) as count FROM vehicles WHERE agency_id = :id GROUP BY statut")
            .bind("id", agencyId)
            .fetch().all()
            .collectMap(row -> (String) row.get("statut"), row -> (Long) row.get("count"))
            .map(DistributionDataDTO::new);
    }

    private Mono<DistributionDataDTO> getRentalStatusDistribution(UUID agencyId) {
        return databaseClient.sql("SELECT status, COUNT(*) as count FROM rentals WHERE agency_id = :id GROUP BY status")
            .bind("id", agencyId)
            .fetch().all()
            .collectMap(row -> (String) row.get("status"), row -> (Long) row.get("count"))
            .map(DistributionDataDTO::new);
    }

    // ==========================================
    // MÉTHODES POUR ORGANISATION (AGRÉGATION)
    // ==========================================

    private Mono<GlobalStatsDTO> getGlobalStatsForOrg(UUID orgId) {
         String sql = """
            SELECT
                (SELECT COUNT(*) FROM agencies WHERE organization_id = :id) as agencies,
                (SELECT COUNT(*) FROM vehicles WHERE organization_id = :id) as vehicles,
                (SELECT COUNT(*) FROM drivers WHERE organization_id = :id) as drivers,
                (SELECT COUNT(*) FROM users WHERE organization_id = :id AND role = 'STAFF') as staff,
                (SELECT COUNT(*) FROM rentals r JOIN agencies a ON r.agency_id = a.id
                    WHERE a.organization_id = :id) as total_rentals,
                (SELECT COUNT(*) FROM rentals r JOIN agencies a ON r.agency_id = a.id
                    WHERE a.organization_id = :id AND r.status = 'ONGOING') as active_rentals,
                (SELECT COUNT(*) FROM rentals r JOIN agencies a ON r.agency_id = a.id
                    WHERE a.organization_id = :id AND r.status = 'RESERVED') as reservations,
                (SELECT COALESCE(SUM(COALESCE(p.rental_portion, p.amount)), 0) FROM payments p
                    JOIN rentals r ON p.rental_id = r.id JOIN agencies a ON r.agency_id = a.id
                    WHERE a.organization_id = :id) as total_rev,
                (SELECT COALESCE(SUM(COALESCE(p.rental_portion, p.amount)), 0) FROM payments p
                    JOIN rentals r ON p.rental_id = r.id JOIN agencies a ON r.agency_id = a.id
                    WHERE a.organization_id = :id
                    AND EXTRACT(MONTH FROM p.transaction_date) = EXTRACT(MONTH FROM CURRENT_DATE)) as month_rev
        """;

        return databaseClient.sql(sql)
            .bind("id", orgId)
            .map(row -> new GlobalStatsDTO(
                row.get("agencies", Long.class),
                row.get("vehicles", Long.class),
                row.get("drivers", Long.class),
                row.get("staff", Long.class),
                row.get("total_rentals", Long.class),
                row.get("active_rentals", Long.class),
                row.get("reservations", Long.class),
                row.get("total_rev", BigDecimal.class),
                row.get("month_rev", BigDecimal.class)
            )).one();
    }

    private Mono<TimeSeriesDataDTO> getRevenueEvolutionForOrg(List<UUID> agencyIds, int year) {
        String sql = """
            SELECT TO_CHAR(transaction_date, 'Mon') as month,
                   SUM(COALESCE(p.rental_portion, p.amount)) as total
            FROM payments p JOIN rentals r ON p.rental_id = r.id
            WHERE r.agency_id IN (:ids) AND EXTRACT(YEAR FROM transaction_date) = :year
            GROUP BY EXTRACT(MONTH FROM transaction_date), TO_CHAR(transaction_date, 'Mon')
            ORDER BY EXTRACT(MONTH FROM transaction_date)
        """;
        return buildTimeSeriesList(sql, agencyIds, year);
    }

    private Mono<TimeSeriesDataDTO> getRentalEvolutionForOrg(List<UUID> agencyIds, int year) {
        String sql = """
            SELECT TO_CHAR(created_at, 'Mon') as month, COUNT(*) as total
            FROM rentals
            WHERE agency_id IN (:ids) AND EXTRACT(YEAR FROM created_at) = :year
            GROUP BY EXTRACT(MONTH FROM created_at), TO_CHAR(created_at, 'Mon')
            ORDER BY EXTRACT(MONTH FROM created_at)
        """;
        return buildTimeSeriesList(sql, agencyIds, year);
    }

    private Mono<DistributionDataDTO> getVehicleStatusDistributionForOrg(List<UUID> agencyIds) {
         return databaseClient.sql(
                "SELECT statut, COUNT(*) as count FROM vehicles WHERE agency_id IN (:ids) GROUP BY statut")
            .bind("ids", agencyIds)
            .fetch().all()
            .collectMap(row -> (String) row.get("statut"), row -> (Long) row.get("count"))
            .map(DistributionDataDTO::new);
    }

    private Mono<DistributionDataDTO> getRentalStatusDistributionForOrg(List<UUID> agencyIds) {
        return databaseClient.sql(
                "SELECT status, COUNT(*) as count FROM rentals WHERE agency_id IN (:ids) GROUP BY status")
            .bind("ids", agencyIds)
            .fetch().all()
            .collectMap(row -> (String) row.get("status"), row -> (Long) row.get("count"))
            .map(DistributionDataDTO::new);
    }

    // ==========================================
    // HELPERS & COMPARAISONS
    // ==========================================

    private Mono<TimeSeriesDataDTO> buildTimeSeries(String sql, UUID id, int year) {
        return databaseClient.sql(sql).bind("id", id).bind("year", year)
            .fetch().all().collectList().map(this::mapRowsToTimeSeries);
    }

    private Mono<TimeSeriesDataDTO> buildTimeSeriesList(String sql, List<UUID> ids, int year) {
        return databaseClient.sql(sql).bind("ids", ids).bind("year", year)
            .fetch().all().collectList().map(this::mapRowsToTimeSeries);
    }

    private TimeSeriesDataDTO mapRowsToTimeSeries(List<Map<String, Object>> rows) {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        rows.forEach(row -> {
            labels.add((String) row.get("month"));
            Object val = row.get("total");
            values.add(val instanceof BigDecimal ? ((BigDecimal) val).doubleValue() : ((Long) val).doubleValue());
        });
        return new TimeSeriesDataDTO(labels, values);
    }

    private Mono<List<AgencyComparisonDTO>> getAgencyComparison(UUID orgId) {
        return agencyRepository.findAllByOrganizationId(orgId)
            .flatMap(agency -> {
                Mono<Long> vehicles = databaseClient.sql("SELECT COUNT(*) FROM vehicles WHERE agency_id = :id")
                    .bind("id", agency.getId()).map(row -> row.get(0, Long.class)).one();

                Mono<Long> rentals = databaseClient.sql("SELECT COUNT(*) FROM rentals WHERE agency_id = :id")
                    .bind("id", agency.getId()).map(row -> row.get(0, Long.class)).one();

                Mono<BigDecimal> revenue = databaseClient.sql(
                        "SELECT COALESCE(SUM(COALESCE(p.rental_portion, p.amount)), 0) FROM payments p "
                        + "JOIN rentals r ON p.rental_id = r.id WHERE r.agency_id = :id")
                    .bind("id", agency.getId()).map(row -> row.get(0, BigDecimal.class)).one();

                return Mono.zip(vehicles, rentals, revenue)
                    .map(t -> new AgencyComparisonDTO(agency.getName(), t.getT1(), t.getT2(), t.getT3()));
            })
            .collectList()
            .map(list -> {
                list.sort(Comparator.comparing(AgencyComparisonDTO::revenue).reversed());
                return list;
            });
    }

    // ==========================================
    // RAPPORTS DÉTAILLÉS (CORRIGÉS)
    // ==========================================

    public Mono<AgencyStatsDTO> getAgencyStats(UUID agencyId, int year, Integer month) {
        String dateFilter = (month == null)
            ? "EXTRACT(YEAR FROM r.created_at) = :year"
            : "EXTRACT(YEAR FROM r.created_at) = :year AND EXTRACT(MONTH FROM r.created_at) = :month";

        String dateFilterNoAlias = (month == null)
            ? "EXTRACT(YEAR FROM created_at) = :year"
            : "EXTRACT(YEAR FROM created_at) = :year AND EXTRACT(MONTH FROM created_at) = :month";

        String revSql = "SELECT COALESCE(SUM(COALESCE(p.rental_portion, p.amount)), 0) FROM payments p "
                + "JOIN rentals r ON p.rental_id = r.id WHERE r.agency_id = :agencyId "
                + "AND EXTRACT(YEAR FROM p.transaction_date) = :year"
                + (month != null ? " AND EXTRACT(MONTH FROM p.transaction_date) = :month" : "");

        DatabaseClient.GenericExecuteSpec revSpec = databaseClient.sql(revSql)
            .bind("agencyId", agencyId)
            .bind("year", year);
        if (month != null) {
            revSpec = revSpec.bind("month", month);
        }

        Mono<BigDecimal> revenueMono = revSpec.map((row, meta) -> row.get(0, BigDecimal.class)).one();

        String countsSql = "SELECT status, COUNT(*) as count FROM rentals WHERE agency_id = :agencyId AND "
                + dateFilterNoAlias + " GROUP BY status";
        DatabaseClient.GenericExecuteSpec countsSpec = databaseClient.sql(countsSql)
            .bind("agencyId", agencyId)
            .bind("year", year);
        if (month != null) {
            countsSpec = countsSpec.bind("month", month);
        }

        Mono<Map<String, Long>> rentalCountsMono = countsSpec.fetch().all().collectMap(
            row -> (String) row.get("status"),
            row -> (Long) row.get("count")
        );

        String topVehSql = "SELECT v.brand || ' ' || v.model as name, COUNT(*) as count "
                + "FROM rentals r JOIN vehicles v ON r.vehicle_id = v.id "
                + "WHERE r.agency_id = :agencyId AND " + dateFilter
                + " GROUP BY v.brand, v.model ORDER BY count DESC LIMIT 5";
        DatabaseClient.GenericExecuteSpec topVehSpec = databaseClient.sql(topVehSql)
            .bind("agencyId", agencyId)
            .bind("year", year);
        if (month != null) {
            topVehSpec = topVehSpec.bind("month", month);
        }

        Mono<Map<String, Long>> topVehiclesMono = topVehSpec.fetch().all().collectMap(
            row -> (String) row.get("name"),
            row -> (Long) row.get("count")
        );

        return Mono.zip(revenueMono, rentalCountsMono, topVehiclesMono, agencyRepository.findById(agencyId))
            .map(tuple -> {
                BigDecimal revenue = tuple.getT1();
                Map<String, Long> counts = tuple.getT2();
                Map<String, Long> vehicles = tuple.getT3();
                var agency = tuple.getT4();

                long total = counts.values().stream().mapToLong(Long::longValue).sum();
                long completed = counts.getOrDefault("COMPLETED", 0L);
                long active = counts.getOrDefault("ONGOING", 0L);
                long cancelled = counts.getOrDefault("CANCELLED", 0L);

                return new AgencyStatsDTO(
                    agency.getName(),
                    revenue,
                    month != null ? revenue : BigDecimal.ZERO,
                    month == null ? revenue : BigDecimal.ZERO,
                    total,
                    active,
                    completed,
                    cancelled,
                    vehicles,
                    Map.of()
                );
            });
    }

    public Mono<OrgStatsDTO> getOrganizationStats(UUID orgId, int year) {
        return agencyRepository.findAllByOrganizationId(orgId)
            .flatMap(agency -> getAgencyStats(agency.getId(), year, null))
            .collectList()
            .map(agencyStatsList -> {
                BigDecimal totalRev = agencyStatsList.stream()
                    .map(AgencyStatsDTO::yearlyRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                long totalRentals = agencyStatsList.stream().mapToLong(AgencyStatsDTO::totalRentals).sum();

                AgencyStatsDTO bestAgency = agencyStatsList.stream()
                    .max(Comparator.comparing(AgencyStatsDTO::yearlyRevenue))
                    .orElse(null);

                return new OrgStatsDTO(
                    totalRev,
                    totalRentals,
                    0L,
                    0L,
                    bestAgency,
                    agencyStatsList
                );
            });
    }

    // ==========================================
    // STATS PLATEFORME (ADMIN)
    // ==========================================

    @Override
    public Mono<PlatformStatsDTO> getPlatformStats() {
        Mono<PlatformStatsDTO.UserCounts> userCountsMono = Mono.zip(
                userRepository.count(),
                userRepository.countByRole("CLIENT"),
                userRepository.countByRole("ORGANIZATION"),
                userRepository.countByAccountType("FREELANCE"),
                userRepository.countByRole("STAFF")
        ).map(t -> new PlatformStatsDTO.UserCounts(t.getT1(), t.getT2(), t.getT3(), t.getT4(), t.getT5()));

        Mono<PlatformStatsDTO.OrgCounts> orgCountsMono = Mono.zip(
                organizationRepository.count(),
                organizationRepository.countByAccountType("COMPANY"),
                organizationRepository.countByAccountType("FREELANCE"),
                organizationRepository.countByGovernanceStatus("REJECTED")
        ).map(t -> new PlatformStatsDTO.OrgCounts(t.getT1(), t.getT2(), t.getT3(), t.getT4()));

        Mono<PlatformStatsDTO.AgencyCounts> agencyCountsMono = Mono.zip(
                agencyRepository.count(),
                organizationRepository.countByAccountType("COMPANY")
        ).map(t -> {
            long totalAgencies = t.getT1();
            long companies = t.getT2();
            double average = companies > 0 ? (double) totalAgencies / companies : 0.0;
            return new PlatformStatsDTO.AgencyCounts(totalAgencies, average);
        });

        Mono<PlatformStatsDTO.VehicleCounts> vehicleCountsMono = Mono.zip(
                vehicleRepository.count(),
                vehicleRepository.countByStatut("AVAILABLE")
        ).map(t -> new PlatformStatsDTO.VehicleCounts(t.getT1(), t.getT2()));

        Mono<PlatformStatsDTO.RentalCounts> rentalCountsMono = Mono.zip(
                rentalRepository.count(),
                rentalRepository.countByStatus(RentalStatus.ONGOING),
                rentalRepository.countByStatus(RentalStatus.COMPLETED),
                rentalRepository.countCompletedThisMonth()
        ).map(t -> new PlatformStatsDTO.RentalCounts(t.getT1(), t.getT2(), t.getT3(), t.getT4()));

        Mono<PlatformStatsDTO.RevenueSummary> revenueMono = Mono.zip(
                subscriptionRepository.sumActivePlanPrices().defaultIfEmpty(BigDecimal.ZERO),
                subscriptionRepository.countActiveOrganizations().defaultIfEmpty(0L)
        ).map(t -> new PlatformStatsDTO.RevenueSummary(
                t.getT1() != null ? t.getT1() : BigDecimal.ZERO,
                t.getT2()
        ));

        return Mono.zip(userCountsMono, orgCountsMono, agencyCountsMono, vehicleCountsMono,
                        rentalCountsMono, revenueMono)
                .map(t -> new PlatformStatsDTO(
                        t.getT1(), t.getT2(), t.getT3(), t.getT4(), t.getT5(), t.getT6()
                ));
    }

    @Override
    public reactor.core.publisher.Flux<com.yowyob.easyrental.modules.statistics.dto.SubscriptionBillingDTO>
            listActiveSubscriptionsBilling() {
        String sql = """
                SELECT s.id            AS subscription_id,
                       s.organization_id,
                       o.name          AS organization_name,
                       o.email         AS organization_email,
                       o.account_type,
                       s.plan_type,
                       p.price,
                       s.status,
                       s.start_date,
                       s.end_date
                FROM subscriptions s
                JOIN organizations o        ON o.id   = s.organization_id
                LEFT JOIN subscription_plans p ON p.name = s.plan_type
                WHERE s.status = 'ACTIVE'
                ORDER BY p.price DESC NULLS LAST, s.start_date DESC
                """;
        return databaseClient.sql(sql)
                .map((row, meta) -> new com.yowyob.easyrental.modules.statistics.dto.SubscriptionBillingDTO(
                        row.get("subscription_id", java.util.UUID.class),
                        row.get("organization_id", java.util.UUID.class),
                        row.get("organization_name", String.class),
                        row.get("organization_email", String.class),
                        row.get("account_type", String.class),
                        row.get("plan_type", String.class),
                        row.get("price", java.math.BigDecimal.class),
                        row.get("status", String.class),
                        row.get("start_date", java.time.LocalDateTime.class),
                        row.get("end_date", java.time.LocalDateTime.class)
                ))
                .all();
    }
}
