package com.yowyob.easyrental.modules.statistics.domain.port.in;

import com.yowyob.easyrental.modules.statistics.dto.AgencyStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.FullDashboardDTO;
import com.yowyob.easyrental.modules.statistics.dto.OrgStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.PlatformStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.SubscriptionBillingDTO;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for statistics use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface StatisticsUseCase {
    Mono<FullDashboardDTO> getAgencyDashboard(UUID agencyId, int year);
    Mono<FullDashboardDTO> getOrganizationDashboard(UUID orgId, int year);
    Mono<AgencyStatsDTO> getAgencyStats(UUID agencyId, int year, Integer month);
    Mono<OrgStatsDTO> getOrganizationStats(UUID orgId, int year);

    Mono<PlatformStatsDTO> getPlatformStats();

    Flux<SubscriptionBillingDTO> listActiveSubscriptionsBilling();
}
