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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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
        String url = kernelProperties.getBaseUrl();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        String finalUrl = url + "/api/organizations/" + kernelOrgId + "/" + action;
        String jsonBody = "{\"reason\":\"" + reason.replace("\"", "\\\"") + "\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(finalUrl))
                .timeout(Duration.ofSeconds(15))
                .header("X-Client-Id", kernelProperties.getClientId())
                .header("X-Api-Key", kernelProperties.getApiKey())
                .header("X-Tenant-Id", kernelProperties.getTenantId())
                .header("X-Organization-Id", kernelOrgId.toString())
                .header("Authorization", "Bearer " + appToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return Mono.fromCallable(() -> {
                    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    boolean ok = resp.statusCode() >= 200 && resp.statusCode() < 300;
                    if (!ok) {
                        log.warn("[admin-org-http] status={} body={}", resp.statusCode(),
                                resp.body() != null && resp.body().length() > 300
                                        ? resp.body().substring(0, 300) + "..." : resp.body());
                    } else {
                        log.info("[admin-org-http] {} status={} OK", action, resp.statusCode());
                    }
                    return ok;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(ex -> {
                    log.error("[admin-org-http] exception : {}", ex.getMessage());
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
