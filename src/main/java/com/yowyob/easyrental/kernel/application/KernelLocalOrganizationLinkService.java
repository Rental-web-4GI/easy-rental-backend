package com.yowyob.easyrental.kernel.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import com.yowyob.easyrental.kernel.security.KernelAuthenticationToken;
import com.yowyob.easyrental.kernel.security.KernelPermissionMapper;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.dto.OrgUpdateDTO;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.shared.exception.ValidationException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Links kernel organizations to local PostgreSQL records when the owner exists but org row is missing.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class KernelLocalOrganizationLinkService {

    private final OrganizationRepositoryPort organizationRepository;
    private final SubscriptionPlanRepositoryPort planRepository;
    private final SubscriptionUseCase subscriptionUseCase;
    private final KernelOrganizationAdapter kernelOrganizationAdapter;
    private final KernelOrganizationBootstrapService kernelOrganizationBootstrapService;
    private final KernelClientProperties kernelProperties;

    /**
     * Ensures a local organization row exists for an owner when kernel JWT already carries an org id.
     *
     * @param owner the authenticated organization owner
     * @param request optional onboarding fields to apply
     * @return linked organization or empty when kernel integration is off or no kernel org is found
     */
    public Mono<OrganizationEntity> ensureLocalOrganization(UserEntity owner, OrgUpdateDTO request) {
        if (!kernelProperties.isIntegrationEnabled()) {
            return Mono.empty();
        }
        return organizationRepository.findByOwnerId(owner.getId())
                .switchIfEmpty(Mono.defer(() -> resolveKernelOrganizationId()
                        .flatMap(kernelOrgId -> organizationRepository.findByKernelOrganizationId(kernelOrgId)
                                .switchIfEmpty(Mono.defer(() -> createLocalFromKernel(owner, request, kernelOrgId))))));
    }

    private Mono<UUID> resolveKernelOrganizationId() {
        return KernelContextHolder.current()
                .flatMap(ctx -> ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> securityContext.getAuthentication())
                        .flatMap(auth -> {
                            if (auth instanceof KernelAuthenticationToken kernelAuth) {
                                if (kernelAuth.getClaims().organizationId().isPresent()) {
                                    return Mono.just(kernelAuth.getClaims().organizationId().get());
                                }
                                Optional<UUID> fromPermissions = KernelPermissionMapper
                                        .organizationIdFromPermissions(kernelAuth.getClaims());
                                if (fromPermissions.isPresent()) {
                                    return Mono.just(fromPermissions.get());
                                }
                            }
                            if (ctx.organizationId().isPresent()) {
                                return Mono.just(ctx.organizationId().get());
                            }
                            if (ctx.bearerToken().isEmpty()) {
                                return Mono.empty();
                            }
                            return kernelOrganizationAdapter.myOrganizations(ctx)
                                    .next()
                                    .mapNotNull(node -> parseUuid(KernelResponseSupport.textOrNull(node, "id")))
                                    .onErrorResume(ex -> Mono.empty());
                        }));
    }

    private Mono<OrganizationEntity> createLocalFromKernel(
            UserEntity owner, OrgUpdateDTO request, UUID kernelOrgId) {
        return KernelContextHolder.current()
                .flatMap(ctx -> planRepository.findByName("FREE")
                        .switchIfEmpty(Mono.error(new ValidationException("Plan FREE not configured")))
                        .flatMap(freePlan -> {
                            KernelRequestContext orgContext = KernelRequestContext.builder()
                                    .bearerToken(ctx.bearerToken())
                                    .organizationId(Optional.of(kernelOrgId))
                                    .agencyId(ctx.agencyId())
                                    .build();
                            Mono<JsonNode> kernelOrgMono = ctx.bearerToken().isPresent()
                                    ? kernelOrganizationAdapter.getOrganization(kernelOrgId, orgContext)
                                            .onErrorResume(ex -> Mono.just(JsonNodeFactory.instance.objectNode()))
                                    : Mono.just(JsonNodeFactory.instance.objectNode());

                            return kernelOrgMono.flatMap(kernelOrg -> {
                                OrganizationEntity org = buildLocalOrganization(
                                        owner, request, kernelOrgId, kernelOrg, freePlan.getId());
                                return organizationRepository.save(Objects.requireNonNull(org))
                                        .flatMap(savedOrg -> kernelOrganizationBootstrapService
                                                .subscribeDefaultServices(kernelOrgId, orgContext)
                                                .onErrorResume(ex -> Mono.empty())
                                                .then(subscriptionUseCase.createHistoryRecord(
                                                        savedOrg.getId(), "FREE", null))
                                                .thenReturn(savedOrg));
                            });
                        }));
    }

    private OrganizationEntity buildLocalOrganization(
            UserEntity owner,
            OrgUpdateDTO request,
            UUID kernelOrgId,
            JsonNode kernelOrg,
            UUID freePlanId) {
        String kernelName = firstNonBlank(
                KernelResponseSupport.textOrNull(kernelOrg, "displayName"),
                KernelResponseSupport.textOrNull(kernelOrg, "legalName"));
        String fallbackName = hasText(owner.getFullname()) ? owner.getFullname().trim() : owner.getEmail();
        String name = request != null && hasText(request.name()) ? request.name() : kernelName;
        if (!hasText(name)) {
            name = fallbackName;
        }

        OrganizationEntity.OrganizationEntityBuilder builder = OrganizationEntity.builder()
                .id(UUID.randomUUID())
                .ownerId(owner.getId())
                .name(name)
                .email(owner.getEmail())
                .country("CM")
                .timezone("Africa/Douala")
                .subscriptionPlanId(freePlanId)
                .subscriptionAutoRenew(true)
                .isVerified(false)
                .isDriverBookingRequired(
                        request != null
                                && request.isDriverBookingRequired() != null
                                && request.isDriverBookingRequired())
                .kernelOrganizationId(kernelOrgId)
                .governanceStatus(kernelOrg.path("governanceStatus").asText("PENDING_APPROVAL"))
                .isNewRecord(true);

        OrganizationEntity org = builder.build();
        if (request != null) {
            applyUpdateFields(org, request);
        }
        boolean isProfileComplete = checkProfileCompleteness(org);
        org.setIsVerified(isProfileComplete);
        org.setVerificationDate(isProfileComplete ? java.time.LocalDateTime.now() : null);
        return org;
    }

    private void applyUpdateFields(OrganizationEntity org, OrgUpdateDTO request) {
        if (hasText(request.name())) {
            org.setName(request.name());
        }
        if (hasText(request.description())) {
            org.setDescription(request.description());
        }
        if (hasText(request.address())) {
            org.setAddress(request.address());
        }
        if (hasText(request.city())) {
            org.setCity(request.city());
        }
        if (hasText(request.postalCode())) {
            org.setPostalCode(request.postalCode());
        }
        if (hasText(request.region())) {
            org.setRegion(request.region());
        }
        if (hasText(request.phone())) {
            org.setPhone(request.phone());
        }
        if (hasText(request.email())) {
            org.setEmail(request.email());
        }
        if (hasText(request.website())) {
            org.setWebsite(request.website());
        }
        if (hasText(request.timezone())) {
            org.setTimezone(request.timezone());
        }
        if (hasText(request.logoUrl())) {
            org.setLogoUrl(request.logoUrl());
        }
        if (hasText(request.registrationNumber())) {
            org.setRegistrationNumber(request.registrationNumber());
        }
        if (hasText(request.taxNumber())) {
            org.setTaxNumber(request.taxNumber());
        }
        if (request.isDriverBookingRequired() != null) {
            org.setIsDriverBookingRequired(request.isDriverBookingRequired());
        }
    }

    private boolean checkProfileCompleteness(OrganizationEntity org) {
        return hasText(org.getName()) && hasText(org.getDescription()) && hasText(org.getAddress())
                && hasText(org.getCity()) && hasText(org.getPhone()) && hasText(org.getEmail())
                && hasText(org.getRegistrationNumber()) && hasText(org.getTaxNumber())
                && hasText(org.getLogoUrl());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty() && !"string".equalsIgnoreCase(value.trim());
    }

    private String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        if (hasText(second)) {
            return second;
        }
        return null;
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }
}
