package com.yowyob.easyrental.modules.organization.infrastructure.adapter.in.web;

import com.yowyob.easyrental.kernel.application.KernelAppTokenProvider;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Endpoints admin plateforme pour la gouvernance des organisations
 * (approbation / rejet). Utilise le token app-level (platform admin)
 * via {@link KernelAppTokenProvider}.
 */
@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Organizations", description = "Gouvernance plateforme des organisations (admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrganizationController {

    private final KernelAppTokenProvider appTokenProvider;
    private final KernelClientProperties kernelProperties;
    private final OrganizationRepositoryPort orgRepository;
    @Qualifier("kernelWebClient")
    private final WebClient kernelWebClient;

    public record GovernanceRequest(String reason) {}

    @Operation(summary = "Approuver une organisation en attente (Admin plateforme)")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<OrganizationEntity>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) GovernanceRequest body) {
        String reason = body != null && body.reason() != null ? body.reason() : "Approved by platform admin";
        return governanceCall(id, reason, /*approve=*/true);
    }

    @Operation(summary = "Rejeter une organisation en attente (Admin plateforme)")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<OrganizationEntity>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) GovernanceRequest body) {
        String reason = body != null && body.reason() != null ? body.reason() : "Rejected by platform admin";
        return governanceCall(id, reason, /*approve=*/false);
    }

    @Operation(summary = "Suspendre une organisation (cascade : agences masquées, login bloqué)")
    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<OrganizationEntity>> suspend(
            @PathVariable UUID id,
            @RequestBody(required = false) GovernanceRequest body) {
        String reason = body != null && body.reason() != null ? body.reason() : "Suspendue par l'administrateur";
        return orgRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable: " + id)))
                .flatMap(org -> {
                    org.setStatus("SUSPENDED");
                    org.setSuspendedAt(java.time.LocalDateTime.now());
                    org.setSuspensionReason(reason);
                    return orgRepository.save(org);
                })
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Réactiver une organisation suspendue")
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<OrganizationEntity>> reactivate(@PathVariable UUID id) {
        return orgRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable: " + id)))
                .flatMap(org -> {
                    org.setStatus("ACTIVE");
                    org.setSuspendedAt(null);
                    org.setSuspensionReason(null);
                    return orgRepository.save(org);
                })
                .map(ResponseEntity::ok);
    }

    private Mono<ResponseEntity<OrganizationEntity>> governanceCall(UUID orgId, String reason, boolean approve) {
        String verb = approve ? "approve" : "reject";
        log.info("[admin-org] {} demandé pour orgId local={}", verb, orgId);
        return orgRepository.findById(orgId)
                .switchIfEmpty(Mono.error(new RuntimeException("Organisation introuvable: " + orgId)))
                .flatMap(org -> {
                    UUID kernelOrgId = org.getKernelOrganizationId();
                    if (kernelOrgId == null) {
                        log.warn("[admin-org] pas de kernelOrgId — update local seulement");
                        return applyLocalStatus(org, approve);
                    }
                    return appTokenProvider.freshToken().flatMap(freshToken -> {
                        if (freshToken.isEmpty()) {
                            log.warn("[admin-org] freshToken() vide — mise à jour locale seulement");
                            return applyLocalStatus(org, approve);
                        }
                        return callKernelGovernance(kernelOrgId, freshToken.get(), reason, approve)
                                .flatMap(success -> {
                                    if (success) {
                                        log.info("[admin-org] {} kernel OK org={} (kernelId={})",
                                                verb, orgId, kernelOrgId);
                                        return applyLocalStatus(org, approve);
                                    }
                                    return Mono.error(new RuntimeException(
                                            "Kernel governance call failed for " + kernelOrgId));
                                });
                    });
                })
                .map(ResponseEntity::ok);
    }

    private Mono<Boolean> callKernelGovernance(UUID kernelOrgId, String appToken, String reason, boolean approve) {
        String action = approve ? "approve" : "reject";
        String uri = "/api/organizations/" + kernelOrgId + "/" + action;
        String safeReason = reason == null ? "" : reason;
        return kernelWebClient.post()
                .uri(uri)
                .headers(h -> {
                    h.set("X-Client-Id", kernelProperties.getClientId());
                    h.set("X-Api-Key", kernelProperties.getApiKey());
                    h.set("X-Tenant-Id", kernelProperties.getTenantId());
                    h.set(HttpHeaders.AUTHORIZATION, "Bearer " + appToken);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("reason", safeReason))
                .exchangeToMono(resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            boolean ok = resp.statusCode().is2xxSuccessful();
                            if (!ok) {
                                String preview = body.length() > 300 ? body.substring(0, 300) + "..." : body;
                                log.warn("[admin-org] {} status={} body={}", action,
                                        resp.statusCode(), preview);
                            } else {
                                log.info("[admin-org] {} status={} OK", action, resp.statusCode());
                            }
                            return ok;
                        }))
                .onErrorResume(ex -> {
                    log.error("[admin-org] exception : {}", ex.getMessage());
                    return Mono.just(false);
                });
    }

    private Mono<OrganizationEntity> applyLocalStatus(OrganizationEntity org, boolean approve) {
        org.setGovernanceStatus(approve ? "APPROVED" : "REJECTED");
        if (approve) {
            org.setIsVerified(true);
        }
        return orgRepository.save(org);
    }
}
