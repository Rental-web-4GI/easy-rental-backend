package com.yowyob.easyrental.modules.statistics.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.statistics.dto.AgencyStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.FullDashboardDTO;
import com.yowyob.easyrental.modules.statistics.dto.OrgStatsDTO;
import com.yowyob.easyrental.modules.statistics.domain.port.in.StatisticsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics & Reporting",
        description = "Endpoints pour les tableaux de bord et rapports financiers/opérationnels")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsController {

    private final StatisticsUseCase statisticsUseCase;

    // =================================================================================
    // 1. DASHBOARDS (Optimisé pour l'affichage graphique Frontend)
    // =================================================================================

    @Operation(summary = "Dashboard Agence (Graphiques & KPIs)",
               description = "Renvoie toutes les données pour le tableau de bord d'une agence "
                       + "(revenus, locations, état du parc).")
    @GetMapping("/agency/{agencyId}/dashboard")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<FullDashboardDTO>> getAgencyDashboard(
            @Parameter(description = "ID de l'agence") @PathVariable UUID agencyId,
            @Parameter(description = "Année cible (défaut: année en cours)")
            @RequestParam(required = false) Integer year) {

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return statisticsUseCase.getAgencyDashboard(agencyId, targetYear)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Dashboard Organisation (Vue d'ensemble & Comparaisons)",
               description = "Données agrégées de toutes les agences et tableaux comparatifs.")
    @GetMapping("/org/{orgId}/dashboard")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<FullDashboardDTO>> getOrgDashboard(
            @Parameter(description = "ID de l'organisation") @PathVariable UUID orgId,
            @Parameter(description = "Année cible (défaut: année en cours)")
            @RequestParam(required = false) Integer year) {

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return statisticsUseCase.getOrganizationDashboard(orgId, targetYear)
                .map(ResponseEntity::ok);
    }

    // =================================================================================
    // 2. RAPPORTS DÉTAILLÉS (Pour les tableaux et exports)
    // =================================================================================

    @Operation(summary = "Rapport détaillé d'une Agence (Revenus, Compteurs)",
               description = "Données chiffrées précises. Peut être filtré par mois pour un bilan mensuel.")
    @GetMapping("/agency/{agencyId}/report")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<AgencyStatsDTO>> getAgencyDetailedReport(
            @PathVariable UUID agencyId,
            @Parameter(description = "Année") @RequestParam(required = false) Integer year,
            @Parameter(description = "Mois (1-12). Si null, renvoie le bilan annuel.")
            @RequestParam(required = false) Integer month) {

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return statisticsUseCase.getAgencyStats(agencyId, targetYear, month)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Rapport global Organisation (Bilan consolidé)",
               description = "Agrégation des revenus et performances de toutes les agences sous forme de liste.")
    @GetMapping("/org/{orgId}/report")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<OrgStatsDTO>> getOrgDetailedReport(
            @PathVariable UUID orgId,
            @Parameter(description = "Année") @RequestParam(required = false) Integer year) {

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return statisticsUseCase.getOrganizationStats(orgId, targetYear)
                .map(ResponseEntity::ok);
    }
}
