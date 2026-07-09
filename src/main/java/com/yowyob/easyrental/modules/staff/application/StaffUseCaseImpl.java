package com.yowyob.easyrental.modules.staff.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelAdministrationAdapter;
import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.in.OrganizationUseCase;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.poste.domain.port.in.PosteUseCase;
import com.yowyob.easyrental.modules.staff.domain.port.in.StaffUseCase;
import com.yowyob.easyrental.modules.staff.domain.port.out.StaffRepositoryPort;
import com.yowyob.easyrental.modules.staff.dto.KernelRoleResponseDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffInviteRequestDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffInviteResponseDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffRequestDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffResponseDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffUpdateDTO;
import com.yowyob.easyrental.modules.staff.mapper.StaffMapper;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffUseCaseImpl implements StaffUseCase {

    private static final String LOCAL_POSTE_CODE = "LOCAL_POSTE";
    private static final Duration KERNEL_ROLES_TIMEOUT = Duration.ofSeconds(12);

    private final StaffRepositoryPort staffRepository;
    private final OrganizationRepositoryPort organizationRepository;
    private final SubscriptionPlanRepositoryPort planRepository;
    private final OrganizationUseCase organizationService;
    private final AgencyRepositoryPort agencyRepository;
    private final PosteUseCase posteService;
    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final KernelClientProperties kernelProperties;
    private final KernelAdministrationAdapter kernelAdministrationAdapter;
    private final KernelStaffProvisioningService kernelStaffProvisioningService;
    private final EasyRentalProperties easyRentalProperties;

    private record ProvisionedStaff(
            UserEntity user,
            KernelStaffProvisioningService.ProvisionResult provision) {
    }

    @Transactional
    public Mono<StaffResponseDTO> addStaffToOrganization(UUID orgId, StaffRequestDTO request) {
        if (kernelProperties.isIntegrationEnabled()) {
            return Mono.error(new ValidationException(
                    "La création directe de staff est désactivée. Utilisez l'invitation kernel (email + rôle)."));
        }
        return staffRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.<UserEntity>error(new RuntimeException("Cet email est déjà utilisé")))
                .switchIfEmpty(Mono.defer(() -> organizationRepository.findById(Objects.requireNonNull(orgId))
                        .switchIfEmpty(Mono.<OrganizationEntity>error(new RuntimeException("Organisation non trouvée")))
                        .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                                .flatMap(plan -> {

                                    int current = (org.getCurrentUsers() != null) ? org.getCurrentUsers() : 0;
                                    int max = (plan.getMaxUsers() != null) ? plan.getMaxUsers() : 0;

                                    if (current >= max) {
                                        return Mono.<UserEntity>error(new RuntimeException(
                                                "Quota atteint : " + current + " / " + max
                                                        + " utilisateurs utilisés."));
                                    }

                                    UserEntity newUser = UserEntity.builder()
                                            .id(UUID.randomUUID())
                                            .firstname(request.firstname())
                                            .lastname(request.lastname())
                                            .fullname(request.firstname() + " " + request.lastname())
                                            .email(request.email())
                                            .password(passwordEncoder.encode("motdepasse"))
                                            .role("STAFF")
                                            .organizationId(orgId)
                                            .agencyId(request.agencyId())
                                            .posteId(request.posteId())
                                            .status("ACTIVE")
                                            .hiredAt(LocalDateTime.now())
                                            .isNewRecord(true)
                                            .build();

                                    return staffRepository.save(Objects.requireNonNull(newUser))
                                            .flatMap(savedUser -> organizationService.updateStaffCounter(orgId, 1)
                                                    .then(updateAgencyStaffCounter(request.agencyId(), 1))
                                                    .thenReturn(savedUser));
                                }))))
                .flatMap(this::enrichStaff);
    }

    @Transactional
    public Mono<StaffInviteResponseDTO> provisionStaffToOrganization(UUID orgId, StaffInviteRequestDTO request) {
        if (!kernelProperties.isIntegrationEnabled()) {
            return Mono.error(new ValidationException(
                    "Staff kernel provisioning requires kernel.integration.enabled=true"));
        }
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .switchIfEmpty(Mono.error(new ValidationException("Organisation non trouvée")))
                .flatMap(org -> {
                    if (org.getKernelOrganizationId() == null) {
                        return Mono.error(new ValidationException(
                                "ORG_NOT_LINKED: Organisation is not linked to kernel"));
                    }
                    if (!"APPROVED".equalsIgnoreCase(org.getGovernanceStatus())
                            && !easyRentalProperties.getStaff().isSkipKernelProvisioning()) {
                        return Mono.error(new ValidationException(
                                "ORG_NOT_APPROVED: Organisation must be approved before inviting staff"));
                    }
                    return Mono.just(org);
                })
                .flatMap(org -> staffRepository.findByEmail(request.email())
                        .flatMap(existing -> {
                            if (orgId.equals(existing.getOrganizationId()) && "STAFF".equals(existing.getRole())) {
                                return Mono.<OrganizationEntity>error(new ValidationException(
                                        "STAFF_ALREADY_EXISTS: This email is already staff in this organisation"));
                            }
                            return Mono.just(org);
                        })
                        .switchIfEmpty(Mono.just(org)))
                .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .flatMap(plan -> {
                            int current = org.getCurrentUsers() != null ? org.getCurrentUsers() : 0;
                            int max = plan.getMaxUsers() != null ? plan.getMaxUsers() : 0;
                            if (current >= max) {
                                return Mono.error(new ValidationException(
                                        "Quota atteint : " + current + " / " + max + " utilisateurs utilisés."));
                            }
                            return Mono.just(org);
                        }))
                .flatMap(org -> agencyRepository.findById(Objects.requireNonNull(request.agencyId()))
                        .switchIfEmpty(Mono.error(new ValidationException("Agence non trouvée")))
                        .flatMap(agency -> {
                            if (!easyRentalProperties.getStaff().isSkipKernelProvisioning()
                                    && agency.getKernelAgencyId() == null) {
                                return Mono.error(new ValidationException(
                                        "AGENCY_NOT_LINKED: Agency must be synced with kernel"));
                            }
                            if (easyRentalProperties.getStaff().isSkipKernelProvisioning()) {
                                return provisionStaffLocally(orgId, request, agency);
                            }
                            return KernelContextHolder.current()
                                    .flatMap(ctx -> resolveInviteKernelRoleId(org, request.kernelRoleId())
                                            .flatMap(kernelRoleId -> kernelStaffProvisioningService
                                                    .provisionKernelUser(request, staffRepository::findByEmail)
                                                    .flatMap(provision -> {
                                                        Mono<UserEntity> savedMono;
                                                        if (provision.kernelUserId() != null) {
                                                            savedMono = kernelStaffProvisioningService.inviteEmployee(
                                                                            org.getKernelOrganizationId(),
                                                                            agency.getKernelAgencyId(),
                                                                            provision.kernelUserId(),
                                                                            kernelRoleId,
                                                                            ctx)
                                                                    .then(persistProvisionedStaff(
                                                                            orgId, request, provision, agency, null))
                                                                    .onErrorResume(ex -> isEmailVerificationPending(ex)
                                                                            && easyRentalProperties.getStaff()
                                                                                    .isSkipEmailVerification()
                                                                            ? persistProvisionedStaff(
                                                                                    orgId, request, provision, agency,
                                                                                    provision.temporaryPassword())
                                                                            : Mono.error(ex));
                                                        } else {
                                                            savedMono = persistProvisionedStaff(
                                                                    orgId, request, provision, agency,
                                                                    provision.temporaryPassword());
                                                        }
                                                        return savedMono.flatMap(saved ->
                                                                kernelStaffProvisioningService
                                                                        .sendOnboardingEmail(request, provision)
                                                                        .thenReturn(new ProvisionedStaff(
                                                                                saved, provision)));
                                                    })));
                        }))
                .flatMap(provisioned -> enrichStaffWithoutPoste(provisioned.user())
                        .map(staff -> toInviteResponse(staff, provisioned.provision())))
                .doOnSuccess(response -> eventPublisher.publishEvent(new AuditEvent(
                        "PROVISION_STAFF", "STAFF",
                        "Staff provisionné : " + response.staff().email())));
    }

    private Mono<ProvisionedStaff> provisionStaffLocally(
            UUID orgId,
            StaffInviteRequestDTO request,
            AgencyEntity agency) {
        String temporaryPassword = TemporaryPasswordGenerator.generate();
        KernelStaffProvisioningService.ProvisionResult provision =
                new KernelStaffProvisioningService.ProvisionResult(null, true, temporaryPassword);
        return persistProvisionedStaff(orgId, request, provision, agency, temporaryPassword)
                .map(saved -> new ProvisionedStaff(saved, provision));
    }

    private StaffInviteResponseDTO toInviteResponse(
            StaffResponseDTO staff,
            KernelStaffProvisioningService.ProvisionResult provision) {
        boolean mailEnabled = easyRentalProperties.getMail().isEnabled();
        String temporaryPassword = null;
        if (!mailEnabled && provision.temporaryPassword() != null
                && !provision.temporaryPassword().isBlank()) {
            temporaryPassword = provision.temporaryPassword();
        }
        return new StaffInviteResponseDTO(
                staff,
                temporaryPassword,
                easyRentalProperties.getAgencyLoginUrl(),
                mailEnabled);
    }

    public Flux<KernelRoleResponseDTO> listKernelRoles(UUID orgId) {
        if (!kernelProperties.isIntegrationEnabled()) {
            return Flux.empty();
        }
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .filter(org -> org.getKernelOrganizationId() != null)
                .flatMapMany(org -> fetchKernelRoles(org)
                        .concatWith(fallbackPosteRoles(orgId))
                        .distinct(KernelRoleResponseDTO::id));
    }

    private Flux<KernelRoleResponseDTO> fetchKernelRoles(OrganizationEntity org) {
        return KernelContextHolder.current()
                .flatMapMany(ctx -> kernelAdministrationAdapter.listRoles(orgKernelContext(org, ctx))
                        .map(this::mapKernelRole)
                        .filter(role -> role.id() != null)
                        .timeout(KERNEL_ROLES_TIMEOUT)
                        .onErrorResume(ex -> Flux.empty()));
    }

    private Flux<KernelRoleResponseDTO> fallbackPosteRoles(UUID orgId) {
        return posteService.getAvailablePostes(orgId)
                .map(poste -> new KernelRoleResponseDTO(poste.id(), poste.name(), LOCAL_POSTE_CODE));
    }

    private Mono<UUID> resolveInviteKernelRoleId(OrganizationEntity org, UUID selectedRoleId) {
        return posteService.getPosteById(selectedRoleId)
                .flatMap(poste -> resolveKernelRoleForLocalPoste(org, poste.name()))
                .switchIfEmpty(Mono.just(selectedRoleId));
    }

    private Mono<UUID> resolveKernelRoleForLocalPoste(OrganizationEntity org, String posteName) {
        return fetchKernelRoles(org)
                .collectList()
                .flatMap(roles -> {
                    if (roles.isEmpty()) {
                        return KernelContextHolder.current()
                                .flatMap(ctx -> kernelAdministrationAdapter
                                        .createDefaultRoles(orgKernelContext(org, ctx))
                                        .then(fetchKernelRoles(org).collectList()));
                    }
                    return Mono.just(roles);
                })
                .flatMap(roles -> {
                    if (roles.isEmpty()) {
                        return Mono.error(new ValidationException(
                                "KERNEL_ROLES_UNAVAILABLE: No kernel role available for staff invitation"));
                    }
                    return Mono.just(pickKernelRoleForPoste(posteName, roles));
                });
    }

    private UUID pickKernelRoleForPoste(String posteName, List<KernelRoleResponseDTO> roles) {
        for (KernelRoleResponseDTO role : roles) {
            if (role.name() != null && role.name().trim().equalsIgnoreCase(posteName)) {
                return role.id();
            }
        }
        for (KernelRoleResponseDTO role : roles) {
            String roleName = role.name() != null ? role.name().toLowerCase() : "";
            if (roleName.contains("agent") || roleName.contains("staff") || roleName.contains("employ")) {
                return role.id();
            }
        }
        for (KernelRoleResponseDTO role : roles) {
            if (!LOCAL_POSTE_CODE.equals(role.code())) {
                return role.id();
            }
        }
        return roles.get(0).id();
    }

    private KernelRequestContext orgKernelContext(OrganizationEntity org, KernelRequestContext ctx) {
        return KernelRequestContext.builder()
                .bearerToken(ctx.bearerToken())
                .organizationId(Optional.of(org.getKernelOrganizationId()))
                .agencyId(ctx.agencyId())
                .build();
    }

    private KernelRoleResponseDTO mapKernelRole(JsonNode node) {
        String idText = KernelResponseSupport.textOrNull(node, "id");
        UUID id = idText != null ? UUID.fromString(idText) : null;
        String name = KernelResponseSupport.textOrNull(node, "name");
        if (name == null) {
            name = KernelResponseSupport.textOrNull(node, "displayName");
        }
        String code = KernelResponseSupport.textOrNull(node, "code");
        return new KernelRoleResponseDTO(id, name, code);
    }

    private Mono<UserEntity> persistProvisionedStaff(
            UUID orgId,
            StaffInviteRequestDTO request,
            KernelStaffProvisioningService.ProvisionResult provision,
            AgencyEntity agency,
            String plainPassword) {
        return staffRepository.findByEmail(request.email())
                .flatMap(existing -> {
                    existing.setFirstname(request.firstname());
                    existing.setLastname(request.lastname());
                    existing.setFullname(request.firstname() + " " + request.lastname());
                    existing.setRole("STAFF");
                    existing.setOrganizationId(orgId);
                    existing.setAgencyId(agency.getId());
                    existing.setPosteId(request.kernelRoleId());
                    if (provision.kernelUserId() != null) {
                        existing.setKernelUserId(provision.kernelUserId());
                    }
                    if (plainPassword != null && !plainPassword.isBlank()) {
                        existing.setPassword(passwordEncoder.encode(plainPassword));
                    }
                    existing.setStatus("ACTIVE");
                    if (existing.getHiredAt() == null) {
                        existing.setHiredAt(LocalDateTime.now());
                    }
                    return staffRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    UserEntity.UserEntityBuilder builder = UserEntity.builder()
                            .id(UUID.randomUUID())
                            .firstname(request.firstname())
                            .lastname(request.lastname())
                            .fullname(request.firstname() + " " + request.lastname())
                            .email(request.email())
                            .role("STAFF")
                            .organizationId(orgId)
                            .agencyId(agency.getId())
                            .posteId(request.kernelRoleId())
                            .status("ACTIVE")
                            .hiredAt(LocalDateTime.now())
                            .isNewRecord(true);
                    if (provision.kernelUserId() != null) {
                        builder.kernelUserId(provision.kernelUserId());
                    }
                    if (plainPassword != null && !plainPassword.isBlank()) {
                        builder.password(passwordEncoder.encode(plainPassword));
                    }
                    UserEntity newUser = builder.build();
                    return staffRepository.save(Objects.requireNonNull(newUser))
                            .flatMap(saved -> organizationService.updateStaffCounter(orgId, 1)
                                    .then(updateAgencyStaffCounter(agency.getId(), 1))
                                    .thenReturn(saved));
                }));
    }

    public Flux<StaffResponseDTO> getStaffByOrganization(UUID orgId) {
        return staffRepository.findAllStaffByOrganizationId(orgId)
                .flatMap(this::enrichStaff);
    }

    public Flux<StaffResponseDTO> getStaffByAgency(UUID agencyId) {
        return staffRepository.findAllStaffByAgencyId(agencyId)
                .flatMap(this::enrichStaff);
    }

    public Mono<StaffResponseDTO> getStaffById(UUID id) {
        return staffRepository.findById(Objects.requireNonNull(id))
                .flatMap(this::enrichStaff)
                .switchIfEmpty(Mono.error(new RuntimeException("Staff non trouvé")));
    }

    @Transactional
    public Mono<Void> deleteStaff(UUID id) {
        return staffRepository.findById(Objects.requireNonNull(id))
                .flatMap(user -> staffRepository.delete(Objects.requireNonNull(user))
                        .then(organizationService.updateStaffCounter(user.getOrganizationId(), -1))
                        .then(updateAgencyStaffCounter(user.getAgencyId(), -1)))
                .doOnSuccess(v -> eventPublisher
                        .publishEvent(new AuditEvent("DELETE_STAFF", "STAFF", "Staff supprimé ID: " + id)));
    }

    private Mono<Void> updateAgencyStaffCounter(UUID agencyId, int increment) {
        return agencyRepository.findById(Objects.requireNonNull(agencyId))
                .flatMap(agency -> {
                    agency.setTotalPersonnel(agency.getTotalPersonnel() + increment);
                    return agencyRepository.save(agency);
                }).then();
    }

    private Mono<StaffResponseDTO> enrichStaff(UserEntity staff) {
        if (staff.getPosteId() == null) {
            return enrichStaffWithoutPoste(staff);
        }
        return posteService.getPosteById(staff.getPosteId())
                .map(posteDto -> staffMapper.toDto(staff, posteDto))
                .onErrorResume(ex -> enrichStaffWithoutPoste(staff));
    }

    private Mono<StaffResponseDTO> enrichStaffWithoutPoste(UserEntity staff) {
        return Mono.just(staffMapper.toDto(staff, null));
    }

    @Transactional
    public Mono<StaffResponseDTO> updateStaff(UUID staffId, StaffUpdateDTO request) {
        return staffRepository.findById(Objects.requireNonNull(staffId))
                .switchIfEmpty(Mono.<UserEntity>error(new RuntimeException("Staff non trouvé")))
                .flatMap(user -> {
                    UUID oldAgencyId = user.getAgencyId();
                    UUID newAgencyId = request.agencyId();

                    Mono<Void> counterUpdate = Mono.empty();
                    if (newAgencyId != null && !newAgencyId.equals(oldAgencyId)) {
                        user.setAgencyId(newAgencyId);
                        counterUpdate = updateAgencyStaffCounter(oldAgencyId, -1)
                                .then(updateAgencyStaffCounter(newAgencyId, 1));
                    }

                    if (request.firstname() != null) {
                        user.setFirstname(request.firstname());
                    }
                    if (request.lastname() != null) {
                        user.setLastname(request.lastname());
                    }
                    if (request.posteId() != null) {
                        user.setPosteId(request.posteId());
                    }
                    if (request.status() != null) {
                        user.setStatus(request.status());
                    }

                    user.setFullname(user.getFirstname() + " " + user.getLastname());

                    return counterUpdate
                            .then(staffRepository.save(user))
                            .flatMap(this::enrichStaff);
                })
                .doOnSuccess(s -> eventPublisher.publishEvent(
                        new AuditEvent("UPDATE_STAFF", "STAFF", "Mise à jour du staff : " + s.email())));
    }

    private static boolean isEmailVerificationPending(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage() != null ? current.getMessage() : "";
            String lower = message.toLowerCase();
            if (lower.contains("not verified")
                    || lower.contains("email_verification")
                    || lower.contains("email_not_verified")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
