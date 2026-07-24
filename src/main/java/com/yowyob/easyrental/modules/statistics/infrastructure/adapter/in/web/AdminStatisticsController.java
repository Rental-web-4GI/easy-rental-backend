package com.yowyob.easyrental.modules.statistics.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.statistics.domain.port.in.StatisticsUseCase;
import com.yowyob.easyrental.modules.statistics.dto.PlatformStatsDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Endpoints admin plateforme pour les statistiques globales (users, orgs, revenu MRR).
 *
 * @author Easy Rental Team
 * @since 2026-07-24
 */
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
@Tag(name = "Admin Statistics", description = "Dashboard statistiques plateforme (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminStatisticsController {

    private final StatisticsUseCase statisticsUseCase;

    @Operation(summary = "Statistiques globales de la plateforme (utilisateurs, organisations, MRR)")
    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<PlatformStatsDTO> getPlatformStats() {
        return statisticsUseCase.getPlatformStats();
    }
}
