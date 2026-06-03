package com.yowyob.easyrental.modules.organization.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.organization.domain.port.in.OrganizationUseCase;
import com.yowyob.easyrental.modules.organization.dto.OrgResponseDTO;
import com.yowyob.easyrental.modules.organization.dto.OrgUpdateDTO;
import com.yowyob.easyrental.modules.organization.dto.OrgUserResponseDTO;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.dto.AutoRenewRequest;
import com.yowyob.easyrental.modules.subscription.dto.PlanUpgradeRequest;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionRemainingTimeDTO;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
@Tag(name = "Organization Management", description = "Endpoints pour la gestion des organisations")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationUseCase organizationUseCase;
    private final SubscriptionUseCase subscriptionUseCase;

    @Operation(summary = "Lister toutes les organisations (Admin)")
    @GetMapping("/all")
    public Flux<OrgResponseDTO> getAll() {
        return organizationUseCase.getAllOrganizations();
    }

    @Operation(summary = "Obtenir les détails d'une organisation")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<OrgResponseDTO>> getOrganization(@PathVariable UUID id) {
        return organizationUseCase.getOrganization(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Récupérer l'utilisateur connecté et son organisation")
    @GetMapping("/auth/me")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<OrgUserResponseDTO>> getMyOrgAndUser() {
        return organizationUseCase.getCurrentOrgAndUser().map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour une organisation (JSON)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<OrgResponseDTO>> updateOrganization(
            @PathVariable UUID id,
            @RequestBody OrgUpdateDTO request) {
        return organizationUseCase.updateOrganization(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour une organisation avec Fichiers (Multipart)")
    @PutMapping(value = "/{id}/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<OrgResponseDTO>> updateOrganizationMultipart(
            @PathVariable UUID id,
            @RequestPart(value = "name", required = false) String name,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "phone", required = false) String phone,
            @RequestPart(value = "email", required = false) String email,
            @RequestPart(value = "address", required = false) String address,
            @RequestPart(value = "city", required = false) String city,
            @RequestPart(value = "postalCode", required = false) String postalCode,
            @RequestPart(value = "region", required = false) String region,
            @RequestPart(value = "website", required = false) String website,
            @RequestPart(value = "registrationNumber", required = false) String registrationNumber,
            @RequestPart(value = "taxNumber", required = false) String taxNumber,
            @RequestPart(value = "isDriverBookingRequired", required = false) Boolean isDriverBookingRequired,
            @RequestPart(value = "logo", required = false) FilePart logoFile,
            @RequestPart(value = "license", required = false) FilePart licenseFile) {
        OrgUpdateDTO partialDto = new OrgUpdateDTO(
                name, description, address, city, postalCode, region, phone, email, website,
                null, null, registrationNumber, taxNumber, isDriverBookingRequired);
        return organizationUseCase.updateOrganizationWithMedia(id, partialDto, logoFile, licenseFile)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les organisations par ID de plan de souscription")
    @GetMapping("/plan/{planId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('ADMIN')")
    public Flux<OrgResponseDTO> getOrganizationsByPlan(@PathVariable UUID planId) {
        return organizationUseCase.getOrganizationsByPlan(planId);
    }

    @Operation(summary = "Activer/Désactiver le renouvellement automatique de l'abonnement")
    @PutMapping("/{id}/subscription/auto-renew")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> toggleAutoRenew(
            @PathVariable UUID id,
            @RequestBody AutoRenewRequest request) {
        return organizationUseCase.toggleAutoRenewWithResponse(id, request.enabled()).map(ResponseEntity::ok);
    }

    @Operation(summary = "Statut de l'abonnement de cette organisation")
    @GetMapping("/{id}/subscription")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> getOrgSubscriptionStatus(@PathVariable UUID id) {
        return organizationUseCase.getOrgSubscriptionStatus(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Temps restant avant expiration")
    @GetMapping("/{id}/subscription/remaining")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionRemainingTimeDTO>> getRemainingTime(@PathVariable UUID id) {
        return subscriptionUseCase.getRemainingTime(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Upgrade du plan de l'organisation")
    @PutMapping("/{id}/subscription/upgrade")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> upgradePlan(
            @PathVariable UUID id,
            @RequestBody PlanUpgradeRequest request) {
        return organizationUseCase.upgradePlanWithResponse(id, request.newPlan().name()).map(ResponseEntity::ok);
    }
}
