package com.yowyob.easyrental.modules.auth.application;

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

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        if (kernelProperties.isIntegrationEnabled()) {
            return tryLocalStaffLogin(request).switchIfEmpty(kernelLogin(request));
        }
        return localLogin(request);
    }

    private Mono<AuthResponse> tryLocalStaffLogin(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(user -> "STAFF".equalsIgnoreCase(user.getRole()))
                .filter(user -> user.getPassword() != null)
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .map(user -> {
                    eventPublisher.publishEvent(new AuditEvent("LOGIN", "AUTH",
                            "Staff logged in locally: " + user.getEmail()));
                    return AuthResponse.withToken(jwtUtil.generateToken(user.getEmail(), user.getRole()));
                });
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
        return userRepository.findByEmail(request.email())
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
        if (oldToken.startsWith("Bearer ")) {
            oldToken = oldToken.substring(7);
        }
        if (jwtUtil.validateToken(oldToken)) {
            String email = jwtUtil.getUsernameFromToken(oldToken);
            return userRepository.findByEmail(email)
                    .map(user -> AuthResponse.withToken(jwtUtil.generateToken(user.getEmail(), user.getRole())));
        }
        if (kernelProperties.isIntegrationEnabled()) {
            return Mono.error(new RuntimeException(
                    "Session expirée. Veuillez vous reconnecter."));
        }
        return Mono.error(new RuntimeException("Invalid Token"));
    }

    @Override
    @Transactional
    public Mono<UserEntity> registerClient(RegisterRequest request) {
        if (kernelProperties.isIntegrationEnabled()) {
            return kernelRegisterClient(request);
        }
        return localRegisterClient(request);
    }

    private Mono<UserEntity> kernelRegisterClient(RegisterRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", kernelProperties.getTenantId());
        payload.put("firstName", request.firstname());
        payload.put("lastName", request.lastname());
        payload.put("username", request.email());
        payload.put("email", request.email());
        payload.put("password", request.password());
        payload.put("accountType", "BUSINESS");

        return kernelAuthAdapter.signUp(payload)
                .flatMap(data -> {
                    String token = data.path("accessToken").asText(null);
                    if (token == null) {
                        return Mono.error(new RuntimeException("Kernel sign-up did not return accessToken"));
                    }
                    return kernelUserMappingService.syncFromAccessToken(token);
                });
    }

    private Mono<UserEntity> localRegisterClient(RegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.<UserEntity>error(new RuntimeException("Email already exists")))
                .switchIfEmpty(Mono.defer(() -> {
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
                                    "New client: " + u.getEmail())));
                }));
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
