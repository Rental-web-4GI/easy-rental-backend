package com.yowyob.easyrental.modules.auth.application;

import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.kernel.application.KernelOrganizationBootstrapService;
import com.yowyob.easyrental.kernel.application.KernelSessionStore;
import com.yowyob.easyrental.kernel.application.KernelUserMappingService;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelAuthAdapter;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.in.AuthUseCase;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.auth.dto.AuthResponse;
import com.yowyob.easyrental.modules.auth.dto.LoginRequest;
import com.yowyob.easyrental.modules.auth.dto.RegisterClientResponse;
import com.yowyob.easyrental.modules.auth.dto.RegisterRequest;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.dto.OrgRegisterRequest;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.shared.exception.ValidationException;
import com.yowyob.easyrental.shared.security.JwtUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Auth use cases with optional kernel-core delegation.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class AuthUseCaseImpl implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final OrganizationRepositoryPort orgRepository;
    private final SubscriptionPlanRepositoryPort planRepository;
    private final SubscriptionUseCase subscriptionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final KernelClientProperties kernelProperties;
    private final KernelAuthAdapter kernelAuthAdapter;
    private final KernelOrganizationAdapter kernelOrganizationAdapter;
    private final KernelOrganizationBootstrapService kernelOrganizationBootstrapService;
    private final KernelUserMappingService kernelUserMappingService;
    private final KernelSessionStore kernelSessionStore;
    private final EasyRentalProperties easyRentalProperties;

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        if (kernelProperties.isIntegrationEnabled()) {
            return tryLocalAdminLogin(request)
                    .switchIfEmpty(tryLocalStaffLogin(request))
                    .switchIfEmpty(tryLocalClientLogin(request))
                    .switchIfEmpty(kernelLogin(request));
        }
        return localLogin(request);
    }

    private Mono<AuthResponse> tryLocalAdminLogin(LoginRequest request) {
        return tryLocalRoleLogin(request, "ADMIN", "Admin logged in locally: ");
    }

    private Mono<AuthResponse> tryLocalStaffLogin(LoginRequest request) {
        return tryLocalRoleLogin(request, "STAFF", "Staff logged in locally: ");
    }

    private Mono<AuthResponse> tryLocalClientLogin(LoginRequest request) {
        if (!easyRentalProperties.getClient().isSkipKernelAuth()) {
            return Mono.empty();
        }
        return tryLocalRoleLogin(request, "CLIENT", "Client logged in locally (skip-kernel-auth): ");
    }

    private Mono<AuthResponse> tryLocalRoleLogin(LoginRequest request, String role, String auditPrefix) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        return userRepository.findByEmail(email)
                .filter(user -> role.equalsIgnoreCase(user.getRole()))
                .filter(user -> user.getPassword() != null)
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .map(user -> {
                    eventPublisher.publishEvent(new AuditEvent("LOGIN", "AUTH", auditPrefix + user.getEmail()));
                    return AuthResponse.withToken(jwtUtil.generateToken(user.getEmail(), user.getRole()));
                });
    }

    private boolean useKernelClientAuth() {
        return kernelProperties.isIntegrationEnabled()
                && !easyRentalProperties.getClient().isSkipKernelAuth();
    }

    @Override
    public Mono<AuthResponse> confirmMfa(String mfaToken, String code) {
        return kernelAuthAdapter.confirmMfa(mfaToken, code)
                .flatMap(result -> kernelUserMappingService.syncFromAccessToken(result.accessToken())
                        .map(user -> issueAuthResponse(user, result.accessToken())));
    }

    private Mono<AuthResponse> kernelLogin(LoginRequest request) {
        String principal = request.email();
        return kernelAuthAdapter.login(principal, request.password())
                .flatMap(result -> {
                    if (result.mfaRequired()) {
                        return Mono.just(AuthResponse.mfaRequired(result.mfaToken(), result.mfaChannel()));
                    }
                    String token = result.accessToken();
                    return kernelUserMappingService.syncFromKernelLogin(principal, token)
                            .map(user -> issueAuthResponse(user, token));
                });
    }

    private AuthResponse issueAuthResponse(UserEntity user, String kernelAccessToken) {
        if ("ORGANIZATION".equalsIgnoreCase(user.getRole())) {
            kernelSessionStore.store(user.getEmail(), kernelAccessToken);
            return AuthResponse.withToken(jwtUtil.generateToken(user.getEmail(), user.getRole()));
        }
        return AuthResponse.withToken(kernelAccessToken);
    }

    private Mono<AuthResponse> localLogin(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword() != null && passwordEncoder.matches(request.password(), u.getPassword()))
                .map(u -> {
                    eventPublisher.publishEvent(new AuditEvent("LOGIN", "AUTH", "User logged in: " + u.getEmail()));
                    return AuthResponse.withToken(jwtUtil.generateToken(u.getEmail(), u.getRole()));
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Bad credentials")));
    }

    @Override
    public Mono<UserEntity> getCurrentUser() {
        if (kernelProperties.isIntegrationEnabled()) {
            return KernelContextHolder.current()
                    .flatMap(ctx -> ctx.bearerToken()
                            .map(token -> {
                                if (jwtUtil.validateToken(token)) {
                                    return localCurrentUser();
                                }
                                return kernelAuthAdapter.getCurrentUser(ctx)
                                        .flatMap(node -> kernelUserMappingService.syncFromKernelUser(node, token));
                            })
                            .orElseGet(this::localCurrentUser));
        }
        return localCurrentUser();
    }

    private Mono<UserEntity> localCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userRepository::findByEmail);
    }

    @Override
    public Mono<AuthResponse> refreshToken(String oldToken) {
        if (oldToken != null && oldToken.startsWith("Bearer ")) {
            oldToken = oldToken.substring(7);
        }
        if (jwtUtil.validateToken(oldToken)) {
            String email = jwtUtil.getUsernameFromToken(oldToken);
            if (kernelProperties.isIntegrationEnabled()) {
                final String finalEmail = email;
                return kernelSessionStore.resolve(finalEmail)
                        .map(Mono::just)
                        .orElseGet(() -> kernelSessionStore.resolveRefreshToken(finalEmail)
                                .map(refreshToken -> kernelAuthAdapter.refresh(refreshToken)
                                        .doOnSuccess(result ->
                                                kernelSessionStore.store(finalEmail, result.accessToken()))
                                        .onErrorResume(ex -> Mono.empty())
                                        .thenReturn("refreshed"))
                                .orElse(Mono.just("no-kernel-session")))
                        .then(userRepository.findByEmail(finalEmail))
                        .map(user -> AuthResponse.withToken(jwtUtil.generateToken(user.getEmail(), user.getRole())));
            }
            return userRepository.findByEmail(email)
                    .map(user -> AuthResponse.withToken(jwtUtil.generateToken(user.getEmail(), user.getRole())));
        }
        return Mono.error(new com.yowyob.easyrental.shared.exception.ValidationException(
                "SESSION_EXPIRED: Token invalide ou expiré. Veuillez vous reconnecter."));
    }

    private static final String CLIENT_EMAIL_VERIFICATION_MESSAGE =
            "Un email de verification a ete envoye. Verifiez votre boite mail avant de vous connecter.";
    private static final String CLIENT_REGISTERED_MESSAGE = "Compte client cree avec succes.";

    @Override
    @Transactional
    public Mono<RegisterClientResponse> registerClient(RegisterRequest request) {
        if (useKernelClientAuth()) {
            return kernelRegisterClient(request);
        }
        return localRegisterClient(request);
    }

    private Mono<RegisterClientResponse> kernelRegisterClient(RegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(this::handleExistingClientOnKernelRegister)
                .switchIfEmpty(Mono.defer(() -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("tenantId", kernelProperties.getTenantId());
                    payload.put("firstName", request.firstname());
                    payload.put("lastName", request.lastname());
                    payload.put("username", request.email());
                    payload.put("email", request.email());
                    payload.put("password", request.password());
                    payload.put("accountType", "BUSINESS");

                    return kernelAuthAdapter.signUp(payload)
                            .flatMap(signUpData -> {
                                String status = signUpData.path("status").asText(null);
                                UUID kernelUserId = parseUuid(signUpData.path("id").asText(null));

                                if ("EMAIL_VERIFICATION_REQUIRED".equals(status)) {
                                    return savePendingKernelClient(request, kernelUserId)
                                            .map(user -> new RegisterClientResponse(
                                                    user,
                                                    true,
                                                    CLIENT_EMAIL_VERIFICATION_MESSAGE));
                                }

                                String token = signUpData.path("accessToken").asText(null);
                                if (token == null) {
                                    return Mono.error(new ValidationException(
                                            "KERNEL_SIGNUP_INCOMPLETE: Kernel sign-up did not return "
                                                    + "an access token."));
                                }
                                return kernelUserMappingService.syncFromAccessToken(token)
                                        .flatMap(user -> applyClientProfile(user, request, kernelUserId))
                                        .map(user -> new RegisterClientResponse(
                                                user,
                                                false,
                                                CLIENT_REGISTERED_MESSAGE));
                            });
                }));
    }

    private Mono<UserEntity> savePendingKernelClient(RegisterRequest request, UUID kernelUserId) {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .firstname(request.firstname())
                .lastname(request.lastname())
                .fullname(request.firstname() + " " + request.lastname())
                .email(request.email())
                .role("CLIENT")
                .kernelUserId(kernelUserId)
                .isNewRecord(true)
                .build();
        return userRepository.save(Objects.requireNonNull(user))
                .doOnSuccess(saved -> eventPublisher.publishEvent(new AuditEvent("REGISTER_CLIENT", "AUTH",
                        "New client pending email verification: " + saved.getEmail())));
    }

    private Mono<UserEntity> applyClientProfile(UserEntity user, RegisterRequest request, UUID kernelUserId) {
        user.setFirstname(request.firstname());
        user.setLastname(request.lastname());
        user.setFullname(request.firstname() + " " + request.lastname());
        user.setRole("CLIENT");
        if (kernelUserId != null) {
            user.setKernelUserId(kernelUserId);
        }
        return userRepository.save(user)
                .doOnSuccess(saved -> eventPublisher.publishEvent(new AuditEvent("REGISTER_CLIENT", "AUTH",
                        "New client: " + saved.getEmail())));
    }

    private Mono<RegisterClientResponse> handleExistingClientOnKernelRegister(UserEntity existing) {
        if ("CLIENT".equalsIgnoreCase(existing.getRole()) && existing.getPassword() == null) {
            return Mono.just(new RegisterClientResponse(
                    existing,
                    true,
                    CLIENT_EMAIL_VERIFICATION_MESSAGE));
        }
        return Mono.error(new ValidationException("Email already exists"));
    }

    private Mono<RegisterClientResponse> localRegisterClient(RegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(existing -> upgradeLocalClientAccount(existing, request))
                .switchIfEmpty(Mono.defer(() -> createLocalClient(request)));
    }

    private Mono<RegisterClientResponse> upgradeLocalClientAccount(UserEntity existing, RegisterRequest request) {
        if (!"CLIENT".equalsIgnoreCase(existing.getRole()) || existing.getPassword() != null) {
            return Mono.error(new ValidationException("Email already exists"));
        }
        existing.setFirstname(request.firstname());
        existing.setLastname(request.lastname());
        existing.setFullname(request.firstname() + " " + request.lastname());
        existing.setPassword(passwordEncoder.encode(request.password()));
        return userRepository.save(existing)
                .doOnSuccess(u -> eventPublisher.publishEvent(new AuditEvent("REGISTER_CLIENT", "AUTH",
                        "Client account upgraded locally: " + u.getEmail())))
                .map(u -> new RegisterClientResponse(u, false, CLIENT_REGISTERED_MESSAGE));
    }

    private Mono<RegisterClientResponse> createLocalClient(RegisterRequest request) {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .firstname(request.firstname())
                .lastname(request.lastname())
                .fullname(request.firstname() + " " + request.lastname())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role("CLIENT")
                .isNewRecord(true)
                .build();
        return userRepository.save(Objects.requireNonNull(user))
                .doOnSuccess(u -> eventPublisher.publishEvent(new AuditEvent("REGISTER_CLIENT", "AUTH",
                        "New client: " + u.getEmail())))
                .map(u -> new RegisterClientResponse(u, false, CLIENT_REGISTERED_MESSAGE));
    }

    @Override
    @Transactional
    public Mono<OrganizationEntity> registerOrganization(OrgRegisterRequest request) {
        if (kernelProperties.isIntegrationEnabled()) {
            return kernelRegisterOrganization(request);
        }
        return localRegisterOrganization(request);
    }

    private Mono<OrganizationEntity> kernelRegisterOrganization(OrgRegisterRequest request) {
        Map<String, Object> signUpPayload = new HashMap<>();
        signUpPayload.put("tenantId", kernelProperties.getTenantId());
        signUpPayload.put("firstName", request.firstname());
        signUpPayload.put("lastName", request.lastname());
        signUpPayload.put("username", request.email());
        signUpPayload.put("email", request.email());
        signUpPayload.put("password", request.password());
        signUpPayload.put("accountType", "BUSINESS");
        signUpPayload.put("businessType", "RETAIL");

        return kernelAuthAdapter.signUp(signUpPayload)
                .flatMap(signUpData -> {
                    String status = signUpData.path("status").asText(null);
                    if ("EMAIL_VERIFICATION_REQUIRED".equals(status)) {
                        return Mono.error(new ValidationException(
                                "EMAIL_NOT_VERIFIED: Account created. Check your email to verify before signing in."));
                    }
                    String token = signUpData.path("accessToken").asText(null);
                    UUID kernelUserId = parseUuid(signUpData.path("id").asText(null));
                    UUID kernelActorId = parseUuid(signUpData.path("actorId").asText(null));
                    if (token == null) {
                        return Mono.error(new ValidationException(
                                "KERNEL_SIGNUP_INCOMPLETE: Kernel sign-up did not return an access token."));
                    }
                    KernelRequestContext userContext = KernelRequestContext.builder()
                            .bearerToken(java.util.Optional.of(token))
                            .build();

                    Map<String, Object> actorPayload = new HashMap<>();
                    actorPayload.put("name", request.firstname() + " " + request.lastname());
                    actorPayload.put("businessId", "ER-" + UUID.randomUUID().toString().substring(0, 8));
                    actorPayload.put("role", "OWNER");
                    actorPayload.put("type", "BUSINESS");
                    actorPayload.put("isIndividual", true);
                    actorPayload.put("isActive", true);

                    Mono<UUID> actorIdMono = kernelActorId != null
                            ? Mono.just(kernelActorId)
                            : kernelOrganizationAdapter.createBusinessActor(actorPayload, userContext)
                                    .map(node -> parseUuid(node.path("id").asText(null)));

                    return actorIdMono.flatMap(actorId ->
                            planRepository.findByName("FREE")
                                    .switchIfEmpty(Mono.error(new RuntimeException("Plan FREE non configuré en base")))
                                    .flatMap(freePlan -> kernelUserMappingService.syncFromAccessToken(token)
                                            .flatMap(savedUser -> {
                                                savedUser.setKernelUserId(kernelUserId);
                                                savedUser.setKernelActorId(actorId);
                                                savedUser.setRole("ORGANIZATION");
                                                return userRepository.save(savedUser);
                                            })
                                            .flatMap(savedUser -> {
                                                Map<String, Object> orgPayload = new HashMap<>();
                                                orgPayload.put("businessActorId", actorId.toString());
                                                orgPayload.put("code", "ORG-" + UUID.randomUUID().toString()
                                                        .substring(0, 8).toUpperCase());
                                                orgPayload.put("legalName", request.orgName());
                                                orgPayload.put("displayName", request.orgName());
                                                orgPayload.put("organizationType", "PRIVATE_COMPANY");

                                                return kernelOrganizationAdapter
                                                        .createOrganization(orgPayload, userContext)
                                                        .flatMap(kernelOrg -> {
                                                            UUID kernelOrgId = parseUuid(
                                                                    kernelOrg.path("id").asText(null));
                                                            String governance = kernelOrg.path("governanceStatus")
                                                                    .asText("PENDING_APPROVAL");

                                                            OrganizationEntity org = OrganizationEntity.builder()
                                                                    .id(UUID.randomUUID())
                                                                    .name(request.orgName())
                                                                    .ownerId(savedUser.getId())
                                                                    .email(savedUser.getEmail())
                                                                    .country("CM")
                                                                    .timezone("Africa/Douala")
                                                                    .subscriptionPlanId(freePlan.getId())
                                                                    .subscriptionAutoRenew(true)
                                                                    .isVerified(false)
                                                                    .isDriverBookingRequired(false)
                                                                    .kernelOrganizationId(kernelOrgId)
                                                                    .governanceStatus(governance)
                                                                    .isNewRecord(true)
                                                                    .build();

                                                            return orgRepository.save(Objects.requireNonNull(org))
                                                                    .flatMap(savedOrg ->
                                                                            kernelOrganizationBootstrapService
                                                                                    .subscribeDefaultServices(
                                                                                            kernelOrgId, userContext)
                                                                                    .then(subscriptionService
                                                                                            .createHistoryRecord(
                                                                                                    savedOrg.getId(),
                                                                                                    freePlan.getName(),
                                                                                                    null))
                                                                                    .thenReturn(savedOrg));
                                                        });
                                            })));
                });
    }

    private Mono<OrganizationEntity> localRegisterOrganization(OrgRegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.<OrganizationEntity>error(new RuntimeException("Email already exists")))
                .switchIfEmpty(Mono.defer(() ->
                        planRepository.findByName("FREE")
                                .switchIfEmpty(Mono.error(new RuntimeException("Plan FREE non configuré en base")))
                                .flatMap(freePlan -> {
                                    UserEntity user = UserEntity.builder()
                                            .id(UUID.randomUUID())
                                            .firstname(request.firstname())
                                            .lastname(request.lastname())
                                            .fullname(request.firstname() + " " + request.lastname())
                                            .email(request.email())
                                            .password(passwordEncoder.encode(request.password()))
                                            .role("ORGANIZATION")
                                            .isNewRecord(true)
                                            .build();

                                    return userRepository.save(Objects.requireNonNull(user)).flatMap(savedUser -> {
                                        OrganizationEntity org = OrganizationEntity.builder()
                                                .id(UUID.randomUUID())
                                                .name(request.orgName())
                                                .ownerId(savedUser.getId())
                                                .email(savedUser.getEmail())
                                                .country("CM")
                                                .timezone("Africa/Douala")
                                                .subscriptionPlanId(freePlan.getId())
                                                .subscriptionAutoRenew(true)
                                                .isVerified(false)
                                                .isDriverBookingRequired(false)
                                                .isNewRecord(true)
                                                .build();

                                        return orgRepository.save(Objects.requireNonNull(org))
                                                .flatMap(savedOrg ->
                                                        subscriptionService.createHistoryRecord(savedOrg.getId(),
                                                                        freePlan.getName(), null)
                                                                .thenReturn(savedOrg))
                                                .doOnSuccess(o -> eventPublisher.publishEvent(
                                                        new AuditEvent("REGISTER_ORG", "AUTH",
                                                                "New Org: " + o.getName() + " with plan FREE")));
                                    });
                                })));
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }
}
