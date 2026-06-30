package com.yowyob.easyrental.modules.organization.application;

import com.yowyob.easyrental.modules.media.domain.MediaEntity;
import com.yowyob.easyrental.modules.media.domain.port.in.MediaUseCase;
import com.yowyob.easyrental.modules.organization.dto.OrgResponseDTO;
import com.yowyob.easyrental.modules.organization.dto.OrgUpdateDTO;
import com.yowyob.easyrental.modules.organization.dto.OrgUserResponseDTO;
import com.yowyob.easyrental.modules.organization.mapper.OrgMapper;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.kernel.application.KernelOrganizationBootstrapService;
import com.yowyob.easyrental.kernel.application.KernelLocalOrganizationLinkService;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import com.yowyob.easyrental.kernel.security.KernelAuthenticationToken;
import com.yowyob.easyrental.shared.exception.ValidationException;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.in.OrganizationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationUseCaseImpl implements OrganizationUseCase {

    private final OrganizationRepositoryPort organizationRepository;
    private final SubscriptionPlanRepositoryPort planRepository;
    private final OrgMapper orgMapper;
    private final MediaUseCase mediaService;
    private final UserRepositoryPort userRepository;
    private final SubscriptionUseCase subscriptionUseCase;
    private final KernelClientProperties kernelProperties;
    private final KernelOrganizationAdapter kernelOrganizationAdapter;
    private final KernelOrganizationBootstrapService kernelOrganizationBootstrapService;
    private final KernelLocalOrganizationLinkService kernelLocalOrganizationLinkService;

    public Mono<OrgResponseDTO> getOrganization(UUID id) {
        return organizationRepository.findById(id)
                .map(orgMapper::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Organization not found")));
    }

    public Flux<OrgResponseDTO> getAllOrganizations() {
        return organizationRepository.findAll().map(orgMapper::toDto);
    }

    @Transactional
    public Mono<OrgResponseDTO> updateOrganization(UUID id, OrgUpdateDTO request) {
        return organizationRepository.findById(id)
                .flatMap(org -> {
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

                    return organizationRepository.save(org);
                })
                .map(orgMapper::toDto);
    }

    @Override
    @Transactional
    public Mono<OrgResponseDTO> completeOnboarding(OrgUpdateDTO request) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(ctx -> resolveCurrentUser(ctx.getAuthentication()))
                .switchIfEmpty(Mono.error(new ValidationException("Authenticated user not found")))
                .flatMap(user -> organizationRepository.findByOwnerId(user.getId())
                        .flatMap(existing -> finalizeOnboardingUpdate(existing, request))
                        .switchIfEmpty(Mono.defer(() -> createOrganizationForOwner(user, request))));
    }

    private Mono<OrgResponseDTO> finalizeOnboardingUpdate(OrganizationEntity org, OrgUpdateDTO request) {
        applyUpdateFields(org, request);
        boolean isProfileComplete = checkProfileCompleteness(org);
        org.setIsVerified(isProfileComplete);
        org.setVerificationDate(isProfileComplete ? java.time.LocalDateTime.now() : null);
        return organizationRepository.save(org).map(orgMapper::toDto);
    }

    private Mono<OrgResponseDTO> createOrganizationForOwner(UserEntity user, OrgUpdateDTO request) {
        if (kernelProperties.isIntegrationEnabled()) {
            return kernelCompleteOnboarding(user, request);
        }
        return localCompleteOnboarding(user, request);
    }

    private Mono<OrgResponseDTO> localCompleteOnboarding(UserEntity user, OrgUpdateDTO request) {
        return planRepository.findByName("FREE")
                .switchIfEmpty(Mono.error(new ValidationException("Plan FREE not configured")))
                .flatMap(freePlan -> {
                    OrganizationEntity org = OrganizationEntity.builder()
                            .id(UUID.randomUUID())
                            .ownerId(user.getId())
                            .country("CM")
                            .subscriptionPlanId(freePlan.getId())
                            .subscriptionAutoRenew(true)
                            .isVerified(false)
                            .isDriverBookingRequired(
                                    request.isDriverBookingRequired() != null && request.isDriverBookingRequired())
                            .isNewRecord(true)
                            .build();
                    applyUpdateFields(org, request);
                    boolean isProfileComplete = checkProfileCompleteness(org);
                    org.setIsVerified(isProfileComplete);
                    org.setVerificationDate(isProfileComplete ? java.time.LocalDateTime.now() : null);
                    return organizationRepository.save(Objects.requireNonNull(org))
                            .flatMap(savedOrg -> subscriptionUseCase
                                    .createHistoryRecord(savedOrg.getId(), freePlan.getName(), null)
                                    .thenReturn(savedOrg));
                })
                .map(orgMapper::toDto);
    }

    private Mono<OrgResponseDTO> kernelCompleteOnboarding(UserEntity user, OrgUpdateDTO request) {
        return kernelLocalOrganizationLinkService.ensureLocalOrganization(user, request)
                .map(orgMapper::toDto)
                .switchIfEmpty(Mono.defer(() -> createKernelOrganizationRemotely(user, request)));
    }

    private Mono<OrgResponseDTO> createKernelOrganizationRemotely(UserEntity user, OrgUpdateDTO request) {
        return KernelContextHolder.current()
                .flatMap(ctx -> {
                    if (ctx.bearerToken().isEmpty()) {
                        return Mono.error(new ValidationException(
                                "Missing kernel session. Please sign in again."));
                    }
                    return Mono.just(ctx);
                })
                .flatMap(ctx -> planRepository.findByName("FREE")
                        .switchIfEmpty(Mono.error(new ValidationException("Plan FREE not configured")))
                        .flatMap(freePlan -> resolveKernelActorId(user, ctx)
                                .flatMap(actorId -> {
                                    Map<String, Object> orgPayload = new HashMap<>();
                                    orgPayload.put("businessActorId", actorId.toString());
                                    orgPayload.put("code", "ORG-" + UUID.randomUUID().toString()
                                            .substring(0, 8).toUpperCase());
                                    orgPayload.put("legalName", request.name());
                                    orgPayload.put("displayName", request.name());
                                    orgPayload.put("organizationType", "PRIVATE_COMPANY");

                                    return kernelOrganizationAdapter.createOrganization(orgPayload, ctx)
                                            .flatMap(kernelOrg -> {
                                                UUID kernelOrgId = parseUuid(
                                                        KernelResponseSupport.textOrNull(kernelOrg, "id"));
                                                String governance = kernelOrg.path("governanceStatus")
                                                        .asText("PENDING_APPROVAL");

                                                OrganizationEntity org = OrganizationEntity.builder()
                                                        .id(UUID.randomUUID())
                                                        .ownerId(user.getId())
                                                        .country("CM")
                                                        .subscriptionPlanId(freePlan.getId())
                                                        .subscriptionAutoRenew(true)
                                                        .isVerified(false)
                                                        .isDriverBookingRequired(
                                                                request.isDriverBookingRequired() != null
                                                                        && request.isDriverBookingRequired())
                                                        .kernelOrganizationId(kernelOrgId)
                                                        .governanceStatus(governance)
                                                        .isNewRecord(true)
                                                        .build();
                                                applyUpdateFields(org, request);
                                                boolean isProfileComplete = checkProfileCompleteness(org);
                                                org.setIsVerified(isProfileComplete);
                                                org.setVerificationDate(isProfileComplete
                                                        ? java.time.LocalDateTime.now() : null);

                                                return organizationRepository.save(Objects.requireNonNull(org))
                                                        .flatMap(savedOrg -> {
                                                            if (user.getKernelActorId() == null) {
                                                                user.setKernelActorId(actorId);
                                                                return userRepository.save(user).thenReturn(savedOrg);
                                                            }
                                                            return Mono.just(savedOrg);
                                                        })
                                                        .flatMap(savedOrg -> kernelOrganizationBootstrapService
                                                                .subscribeDefaultServices(kernelOrgId, ctx)
                                                                .then(subscriptionUseCase
                                                                        .createHistoryRecord(savedOrg.getId(),
                                                                                freePlan.getName(), null))
                                                                .thenReturn(savedOrg));
                                            });
                                })))
                .map(orgMapper::toDto);
    }

    private Mono<UUID> resolveKernelActorId(UserEntity user, KernelRequestContext context) {
        if (user.getKernelActorId() != null) {
            return Mono.just(user.getKernelActorId());
        }
        Map<String, Object> actorPayload = new HashMap<>();
        String displayName = hasText(user.getFullname()) ? user.getFullname().trim() : user.getEmail();
        actorPayload.put("name", displayName);
        actorPayload.put("businessId", "ER-" + UUID.randomUUID().toString().substring(0, 8));
        actorPayload.put("role", "OWNER");
        actorPayload.put("type", "BUSINESS");
        actorPayload.put("isIndividual", true);
        actorPayload.put("isActive", true);
        return kernelOrganizationAdapter.createBusinessActor(actorPayload, context)
                .map(node -> parseUuid(KernelResponseSupport.textOrNull(node, "id")));
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

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    @Transactional
    public Mono<OrgResponseDTO> updateOrganizationWithMedia(UUID id, OrgUpdateDTO request, FilePart logoFile,
            FilePart licenseFile) {
        return organizationRepository.findById(id)
                .flatMap(org -> {
                    Mono<String> logoMono = (logoFile != null)
                            ? mediaService.uploadFile(logoFile).map(MediaEntity::getFileUrl)
                            : Mono.justOrEmpty(org.getLogoUrl());

                    Mono<String> licenseMono = (licenseFile != null)
                            ? mediaService.uploadFile(licenseFile).map(MediaEntity::getFileUrl)
                            : Mono.justOrEmpty(org.getBusinessLicense());

                    return Mono.zip(logoMono.defaultIfEmpty(""), licenseMono.defaultIfEmpty(""))
                            .flatMap(tuple -> {
                                String newLogoUrl = tuple.getT1();
                                String newLicenseUrl = tuple.getT2();

                                // CORRECTION : On ne met à jour que si le texte n'est pas vide
                                if (hasText(request.name())) {
                                    org.setName(request.name());
                                }
                                if (hasText(request.description())) {
                                    org.setDescription(request.description());
                                }
                                if (hasText(request.phone())) {
                                    org.setPhone(request.phone());
                                }
                                if (hasText(request.email())) {
                                    org.setEmail(request.email());
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
                                if (hasText(request.website())) {
                                    org.setWebsite(request.website());
                                }
                                if (hasText(request.timezone())) {
                                    org.setTimezone(request.timezone());
                                }
                                if (hasText(request.registrationNumber())) {
                                    org.setRegistrationNumber(request.registrationNumber());
                                }
                                if (hasText(request.taxNumber())) {
                                    org.setTaxNumber(request.taxNumber());
                                }

                                if (hasText(newLogoUrl)) {
                                    org.setLogoUrl(newLogoUrl);
                                }
                                if (hasText(newLicenseUrl)) {
                                    org.setBusinessLicense(newLicenseUrl);
                                }

                                boolean isProfileComplete = checkProfileCompleteness(org);
                                org.setIsVerified(isProfileComplete);
                                org.setVerificationDate(isProfileComplete ? java.time.LocalDateTime.now() : null);

                                return organizationRepository.save(org);
                            });
                })
                .map(orgMapper::toDto);
    }

    private boolean checkProfileCompleteness(com.yowyob.easyrental.modules.organization.domain.OrganizationEntity org) {
        return hasText(org.getName()) && hasText(org.getDescription()) && hasText(org.getAddress()) &&
               hasText(org.getCity()) && hasText(org.getPhone()) && hasText(org.getEmail()) &&
               hasText(org.getRegistrationNumber()) && hasText(org.getTaxNumber()) && hasText(org.getLogoUrl());
    }

    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    @Override
    @Transactional public Mono<Void> updateAgencyCounter(UUID orgId, int increment) {
        return organizationRepository.findById(orgId).flatMap(org -> { org.setCurrentAgencies(org
                .getCurrentAgencies() + increment); return organizationRepository.save(org); }).then();
    }
    @Override
    @Transactional public Mono<Void> updateStaffCounter(UUID orgId, int increment) {
        return organizationRepository.findById(orgId).flatMap(org -> { org.setCurrentUsers(org
                .getCurrentUsers() + increment); return organizationRepository.save(org); }).then();
    }
    @Override
    @Transactional public Mono<Void> updateVehicleCounter(UUID orgId, int increment) {
        return organizationRepository.findById(orgId).flatMap(org -> { org.setCurrentVehicles(org
                .getCurrentVehicles() + increment); return organizationRepository.save(org); }).then();
    }
    @Override
    @Transactional public Mono<Void> updateDriverCounter(UUID orgId, int increment) {
        return organizationRepository.findById(orgId).flatMap(org -> { org.setCurrentDrivers(org
                .getCurrentDrivers() + increment); return organizationRepository.save(org); }).then();
    }

    public Mono<Boolean> validateQuota(UUID orgId, String resourceType) {
        return organizationRepository.findById(orgId)
                .flatMap(org -> planRepository.findById(org.getSubscriptionPlanId())
                        .map(plan -> switch (resourceType.toUpperCase()) {
                            case "AGENCY" -> org.getCurrentAgencies() < plan.getMaxAgencies();
                            case "VEHICLE" -> org.getCurrentVehicles() < plan.getMaxVehicles();
                            case "DRIVER" -> org.getCurrentDrivers() < plan.getMaxDrivers();
                            case "STAFF", "USER" -> org.getCurrentUsers() < plan.getMaxUsers();
                            default -> false;
                        }))
                .defaultIfEmpty(false);
    }

    public Mono<OrgUserResponseDTO> getCurrentOrgAndUser() {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> resolveCurrentUser(ctx.getAuthentication()))
            .flatMap(user -> organizationRepository.findByOwnerId(user.getId())
                    .switchIfEmpty(Mono.defer(() -> kernelLocalOrganizationLinkService
                            .ensureLocalOrganization(user, null)))
                    .flatMap(this::syncGovernanceFromKernelIfNeeded)
                    .map(org -> new OrgUserResponseDTO(user, orgMapper.toDto(org)))
                    .defaultIfEmpty(new OrgUserResponseDTO(user, null)));
    }

    private Mono<OrganizationEntity> syncGovernanceFromKernelIfNeeded(OrganizationEntity org) {
        if (!kernelProperties.isIntegrationEnabled() || org.getKernelOrganizationId() == null) {
            return Mono.just(org);
        }
        return KernelContextHolder.current()
                .flatMap(ctx -> {
                    if (ctx.bearerToken().isEmpty()) {
                        return Mono.just(org);
                    }
                    return kernelOrganizationAdapter.getOrganization(org.getKernelOrganizationId(), ctx)
                            .map(kernelOrg -> KernelResponseSupport.textOrNull(kernelOrg, "governanceStatus"))
                            .flatMap(kernelGovernance -> {
                                if (kernelGovernance == null
                                        || kernelGovernance.equals(org.getGovernanceStatus())) {
                                    return Mono.just(org);
                                }
                                org.setGovernanceStatus(kernelGovernance);
                                return organizationRepository.save(org);
                            })
                            .onErrorResume(ex -> Mono.just(org));
                });
    }

    private Mono<UserEntity> resolveCurrentUser(org.springframework.security.core.Authentication authentication) {
        if (authentication instanceof KernelAuthenticationToken kernelAuth) {
            try {
                UUID kernelUserId = UUID.fromString(kernelAuth.getClaims().subject());
                return userRepository.findByKernelUserId(kernelUserId);
            } catch (IllegalArgumentException ex) {
                return Mono.empty();
            }
        }
        return userRepository.findByEmail(authentication.getName());
    }

    @Override
    public Flux<OrgResponseDTO> getOrganizationsByPlan(UUID planId) {
        return organizationRepository.findAllBySubscriptionPlanId(planId).map(orgMapper::toDto);
    }

    @Override
    public Mono<SubscriptionResponseDTO> toggleAutoRenewWithResponse(UUID orgId, boolean enabled) {
        return subscriptionUseCase.toggleAutoRenew(orgId, enabled)
                .flatMap(subscriptionUseCase::buildSubscriptionResponse);
    }

    @Override
    public Mono<SubscriptionResponseDTO> getOrgSubscriptionStatus(UUID orgId) {
        return subscriptionUseCase.getOrgSubscriptionStatus(orgId);
    }

    @Override
    public Mono<SubscriptionResponseDTO> upgradePlanWithResponse(UUID orgId, String planName) {
        return subscriptionUseCase.upgradePlan(orgId, planName)
                .flatMap(plan -> organizationRepository.findById(orgId)
                        .flatMap(subscriptionUseCase::buildSubscriptionResponse));
    }
}
