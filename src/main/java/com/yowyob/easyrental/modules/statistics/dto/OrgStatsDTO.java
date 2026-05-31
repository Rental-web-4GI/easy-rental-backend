package com.yowyob.easyrental.modules.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrgStatsDTO(
    BigDecimal globalRevenue,
    Long totalRentals,
    Long totalVehicles,
    Long totalDrivers,
    AgencyStatsDTO bestPerformingAgency,
    List<AgencyStatsDTO> agenciesBreakdown
) {}
