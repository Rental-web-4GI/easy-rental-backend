package com.yowyob.easyrental.modules.organization.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.organization.dto.OrgResponseDTO;
import com.yowyob.easyrental.modules.organization.dto.OrgUpdateDTO;
import com.yowyob.easyrental.modules.organization.dto.OrgUserResponseDTO;
import com.yowyob.easyrental.modules.organization.mapper.OrgMapper;
import com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence.OrganizationRepository;
import com.yowyob.easyrental.modules.organization.application.OrganizationUseCaseImpl;
import com.yowyob.easyrental.modules.subscription.dto.AutoRenewRequest;
import com.yowyob.easyrental.modules.subscription.dto.PlanUpgradeRequest;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionRemainingTimeDTO;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import com.yowyob.easyrental.modules.subscription.mapper.SubscriptionMapper;
import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionPlanRepository;
import com.yowyob.easyrental.modules.subscription.application.SubscriptionUseCaseImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
@Tag(name = "Organization Management", description = "Endpoints pour la gestion des organisations")
@SecurityRequirement(name = "bearerAuth")
public class OrganizationController {

    private final OrganizationUseCaseImpl organizationUseCaseImpl;
    private final OrganizationRepository organizationRepository;
    private final OrgMapper orgMapper;

    private final SubscriptionUseCaseImpl subscriptionUseCaseImpl;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Operation(summary = "Lister toutes les organisations (Admin)")
    @GetMapping("/all")
//     @PreAuthorize("hasRole('ADMIN')")
    public Flux<OrgResponseDTO> getAll() {
        return organizationUseCaseImpl.getAllOrganizations();
    }

    @Operation(summary = "Obtenir les détails d'une organisation")
    @GetMapping("/{id}")
//     @PreAuthorize("hasRole('ORGANIZATION') or hasRole('ADMIN')")
    public Mono<ResponseEntity<OrgResponseDTO>> getOrganization(@PathVariable UUID id) {
        return organizationUseCaseImpl.getOrganization(id)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Récupérer l'utilisateur connecté et son organisation")
    @GetMapping("/auth/me")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<OrgUserResponseDTO>> getMyOrgAndUser() {
        return organizationUseCaseImpl.getCurrentOrgAndUser()
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour une organisation (JSON)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<OrgResponseDTO>> updateOrganization(
            @PathVariable UUID id,
            @RequestBody OrgUpdateDTO request) {
        return organizationUseCaseImpl.updateOrganization(id, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour une organisation avec Fichiers (Multipart)")
    @PutMapping(value = "/{id}/multipart", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<OrgResponseDTO>> updateOrganizationMultipart(
            @PathVariable UUID id,
            // Champs texte facultatifs
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
            // Fichiers facultatifs
            @RequestPart(value = "logo", required = false) FilePart logoFile,
            @RequestPart(value = "license", required = false) FilePart licenseFile
    ) {
        // Reconstruction manuelle du DTO à partir des parts
        // Note: Les champs non présents seront null, ce qui est géré par le service
        OrgUpdateDTO partialDto = new OrgUpdateDTO(
                name, description, address, city, postalCode, region, phone, email, website,
                null, // timezone géré à part ou via autre endpoint si complexe
                null, // logoUrl géré par le fichier
                registrationNumber, taxNumber, isDriverBookingRequired // isDriverBookingRequired
        );

        return organizationUseCaseImpl.updateOrganizationWithMedia(id, partialDto, logoFile, licenseFile)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les organisations par ID de plan de souscription")
    @GetMapping("/plan/{planId}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('ADMIN')")
    public Flux<OrgResponseDTO> getOrganizationsByPlan(@PathVariable UUID planId) {
        return organizationRepository.findAllBySubscriptionPlanId(planId)
                .map(orgMapper::toDto);
    }

        @Operation(summary = "Activer/Désactiver le renouvellement automatique de l'abonnement")
        @PutMapping("/{id}/subscription/auto-renew")
        @PreAuthorize("hasRole('ORGANIZATION')")
        public Mono<ResponseEntity<SubscriptionResponseDTO>> toggleAutoRenew(
                @PathVariable UUID id,
                @RequestBody AutoRenewRequest request) {

        return subscriptionUseCaseImpl.toggleAutoRenew(id, request.enabled())
                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .map(plan -> subscriptionMapper.toResponseDTO(org, plan)))
                .map(ResponseEntity::ok);
        }

    @Operation(summary = "Statut de l'abonnement de cette organisation")
    @GetMapping("/{id}/subscription")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> getOrgSubscriptionStatus(@PathVariable UUID id) {
        return organizationRepository.findById(Objects.requireNonNull(id))
                .flatMap(subscriptionUseCaseImpl::checkAndDowngrade)
                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .map(plan -> subscriptionMapper.toResponseDTO(org, plan)))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Temps restant avant expiration")
    @GetMapping("/{id}/subscription/remaining")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionRemainingTimeDTO>> getRemainingTime(@PathVariable UUID id) {
        return subscriptionUseCaseImpl.getRemainingTime(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Upgrade du plan de l'organisation")
    @PutMapping("/{id}/subscription/upgrade")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<SubscriptionResponseDTO>> upgradePlan(
            @PathVariable UUID id,
            @RequestBody PlanUpgradeRequest request) {
        return subscriptionUseCaseImpl.upgradePlan(id, request.newPlan().name())
                .flatMap(updatedPlan -> organizationRepository.findById(Objects.requireNonNull(id))
                        .map(org -> subscriptionMapper.toResponseDTO(org, updatedPlan)))
                .map(ResponseEntity::ok);
    }
}
