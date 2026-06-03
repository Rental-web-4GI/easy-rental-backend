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
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.port.in.OrganizationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
            .map(ctx -> ctx.getAuthentication().getName())
            .flatMap(userRepository::findByEmail)
            .flatMap(user -> organizationRepository.findByOwnerId(user.getId())
                    .map(org -> new OrgUserResponseDTO(user, orgMapper.toDto(org)))
                    .defaultIfEmpty(new OrgUserResponseDTO(user, null)));
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
