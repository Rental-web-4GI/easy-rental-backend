package com.yowyob.easyrental.modules.auth.application;

import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.kernel.application.KernelOrganizationBootstrapService;
import com.yowyob.easyrental.kernel.application.KernelOwnerAssignmentService;
import com.yowyob.easyrental.kernel.application.KernelSessionStore;
import com.yowyob.easyrental.kernel.application.KernelUsernameGenerator;
import com.yowyob.easyrental.kernel.application.KernelUserMappingService;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelAuthAdapter;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelTpAdapter;
import com.yowyob.easyrental.modules.audit.domain.port.in.AuditUseCase;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.in.AuthUseCase;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.auth.dto.AuthResponse;
import com.yowyob.easyrental.modules.auth.dto.LoginRequest;
import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.auth.dto.RegisterClientResponse;
import com.yowyob.easyrental.modules.auth.dto.RegisterFreelanceRequest;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    private final KernelTpAdapter kernelTpAdapter;
    private final AgencyRepositoryPort agencyRepository;
    private final KernelOwnerAssignmentService kernelOwnerAssignmentService;
    private final AuditUseCase auditUseCase;

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        Mono<AuthResponse> flow;
        if (kernelProperties.isIntegrationEnabled()) {
            flow = tryLocalAdminLogin(request)
                    .switchIfEmpty(tryLocalStaffLogin(request))
                    .switchIfEmpty(tryLocalClientLogin(request))
                    .switchIfEmpty(kernelLogin(request));
        } else {
            flow = localLogin(request);
        }
        // onErrorResume (au lieu de doOnError) pour que l'audit s'exécute DANS la
        // chaîne réactive — il capte ainsi l'IP/user-agent du contexte (AuditContextFilter).
        return flow.onErrorResume(error ->
                auditLoginFailed(request).then(Mono.error(error)));
    }

    private Mono<Void> auditLoginFailed(LoginRequest request) {
        return auditUseCase.record(null, "LOGIN_FAILED", null, null, null, null,
                buildLoginMetadata(request, null));
    }

    /**
     * R3 : refuse la connexion des membres (owner/staff) d'une organisation suspendue.
     * Passe silencieusement pour les autres rôles ou si l'organisation est active/introuvable.
     */
    private Mono<Void> ensureOrgActive(UserEntity user) {
        String role = user.getRole();
        if (!"ORGANIZATION".equalsIgnoreCase(role) && !"STAFF".equalsIgnoreCase(role)) {
            return Mono.empty();
        }
        Mono<OrganizationEntity> orgMono = user.getOrganizationId() != null
                ? orgRepository.findById(user.getOrganizationId())
                : orgRepository.findByOwnerId(user.getId());
        return orgMono
                .filter(org -> "SUSPENDED".equalsIgnoreCase(org.getStatus()))
                .flatMap(org -> Mono.<Void>error(new ValidationException("ORG_SUSPENDED")))
                .then();
    }

    private String buildLoginMetadata(LoginRequest request, String role) {
        String email = (request == null || request.email() == null) ? "" : request.email().trim();
        String source = (request == null || request.source() == null) ? "" : request.source().trim().toUpperCase();
        StringBuilder sb = new StringBuilder("{\"email\":\"")
                .append(email.replace("\"", "\\\""))
                .append("\"");
        if (!source.isBlank()) {
            sb.append(",\"source\":\"").append(source.replace("\"", "\\\"")).append("\"");
        }
        if (role != null && !role.isBlank()) {
            sb.append(",\"role\":\"").append(role).append("\"");
        }
        return sb.append("}").toString();
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
                .flatMap(user -> ensureOrgActive(user).then(Mono.defer(() -> {
                    eventPublisher.publishEvent(new AuditEvent("LOGIN", "AUTH", auditPrefix + user.getEmail()));
                    return auditUseCase.record(user.getId(), "LOGIN_SUCCESS", "USER", user.getId(), null, null,
                            buildLoginMetadata(request, role))
                        .thenReturn(AuthResponse.withToken(jwtUtil.generateToken(user.getEmail(), user.getRole())));
                })));
    }

    private boolean useKernelClientAuth() {
        return kernelProperties.isIntegrationEnabled()
                && !easyRentalProperties.getClient().isSkipKernelAuth();
    }

    @Override
    public Mono<AuthResponse> confirmMfa(String mfaToken, String code) {
        return kernelAuthAdapter.confirmMfa(mfaToken, code)
                .flatMap(result -> kernelUserMappingService.syncFromAccessToken(result.accessToken())
                        .flatMap(user -> issueAuthResponse(user, result.accessToken(), null)));
    }

    private Mono<AuthResponse> kernelLogin(LoginRequest request) {
        // L'utilisateur saisit son email dans l'UI, mais Kernel attend le username
        // qu'on a stocké dans kernel_principal au moment du signup. On regarde
        // d'abord en base locale ; si pas de mapping (compte antérieur au patch),
        // on tente avec l'email tel quel comme fallback.
        String email = request.email() == null ? "" : request.email().trim();
        return userRepository.findByEmail(email)
                .map(user -> {
                    String stored = user.getKernelPrincipal();
                    return stored != null && !stored.isBlank() ? stored : email;
                })
                .defaultIfEmpty(email)
                .flatMap(principal -> kernelAuthAdapter.login(principal, request.password())
                        .flatMap(result -> {
                            if (result.mfaRequired()) {
                                return Mono.just(AuthResponse.mfaRequired(result.mfaToken(), result.mfaChannel()));
                            }
                            String token = result.accessToken();
                            return kernelUserMappingService.syncFromKernelLogin(principal, token)
                                    .flatMap(user -> issueAuthResponse(user, token, request));
                        }));
    }

    private Mono<AuthResponse> issueAuthResponse(UserEntity user, String kernelAccessToken, LoginRequest request) {
        AuthResponse response;
        if ("ORGANIZATION".equalsIgnoreCase(user.getRole())) {
            kernelSessionStore.store(user.getEmail(), kernelAccessToken);
            response = AuthResponse.withToken(jwtUtil.generateToken(user.getEmail(), user.getRole()));
        } else {
            response = AuthResponse.withToken(kernelAccessToken);
        }
        return ensureOrgActive(user).then(
                auditUseCase.record(user.getId(), "LOGIN_SUCCESS", "USER", user.getId(), null, null,
                        buildLoginMetadata(request, user.getRole())).thenReturn(response));
    }

    private Mono<AuthResponse> localLogin(LoginRequest request) {
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        return userRepository.findByEmail(email)
                .filter(u -> u.getPassword() != null && passwordEncoder.matches(request.password(), u.getPassword()))
                .flatMap(u -> ensureOrgActive(u).then(Mono.defer(() -> {
                    eventPublisher.publishEvent(new AuditEvent("LOGIN", "AUTH", "User logged in: " + u.getEmail()));
                    return auditUseCase.record(u.getId(), "LOGIN_SUCCESS", "USER", u.getId(), null, null,
                            buildLoginMetadata(request, u.getRole()))
                        .thenReturn(AuthResponse.withToken(jwtUtil.generateToken(u.getEmail(), u.getRole())));
                })))
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
    @Transactional(noRollbackFor = ValidationException.class)
    public Mono<RegisterClientResponse> registerClient(RegisterRequest request) {
        Mono<RegisterClientResponse> flow = useKernelClientAuth()
                ? kernelRegisterClient(request)
                : localRegisterClient(request);
        return flow
                .doOnSuccess(resp -> {
                    if (resp != null && resp.user() != null) {
                        auditUseCase.record(resp.user().getId(), "SIGNUP_CLIENT_SUCCESS", "USER",
                                resp.user().getId(), null, null, null).subscribe();
                    }
                })
                .doOnError(err -> auditUseCase.record(null, "SIGNUP_CLIENT_FAILED", null, null, null, null,
                        signupFailureMetadata(request.email(), err)).subscribe());
    }

    private String signupFailureMetadata(String email, Throwable err) {
        String safeEmail = email == null ? "" : email.replace("\"", "\\\"");
        String reason = err != null && err.getMessage() != null
                ? err.getMessage().replace("\"", "\\\"")
                : "";
        return "{\"email\":\"" + safeEmail + "\",\"reason\":\"" + reason + "\"}";
    }

    private Mono<RegisterClientResponse> kernelRegisterClient(RegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(this::handleExistingClientOnKernelRegister)
                .switchIfEmpty(Mono.defer(() -> {
                    // Le pattern Kernel exige un username [A-Za-z0-9._-] sans @ :
                    // envoyer l'email complet fait générer un placeholder pending-<uuid> par Kernel.
                    String kernelUsername = KernelUsernameGenerator.fromEmail(request.email());

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("tenantId", kernelProperties.getTenantId());
                    payload.put("firstName", request.firstname());
                    payload.put("lastName", request.lastname());
                    payload.put("username", kernelUsername);
                    payload.put("email", request.email());
                    payload.put("password", request.password());
                    payload.put("accountType", "BUSINESS");

                    return kernelAuthAdapter.signUp(payload)
                            .flatMap(signUpData -> {
                                String status = signUpData.path("status").asText(null);
                                UUID kernelUserId = parseUuid(signUpData.path("id").asText(null));
                                // Récupérer le username effectivement retenu par Kernel
                                // (il peut différer si collision : suffix -2 etc.)
                                String effectivePrincipal = signUpData.path("username").asText(kernelUsername);

                                if ("EMAIL_VERIFICATION_REQUIRED".equals(status)) {
                                    return savePendingKernelClient(request, kernelUserId, effectivePrincipal)
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
                                        .flatMap(user -> applyClientProfile(user, request,
                                                kernelUserId, effectivePrincipal))
                                        .map(user -> new RegisterClientResponse(
                                                user,
                                                false,
                                                CLIENT_REGISTERED_MESSAGE));
                            });
                }));
    }

    private Mono<UserEntity> savePendingKernelClient(RegisterRequest request, UUID kernelUserId,
                                                     String kernelPrincipal) {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .firstname(request.firstname())
                .lastname(request.lastname())
                .fullname(request.firstname() + " " + request.lastname())
                .email(request.email())
                .role("CLIENT")
                .kernelUserId(kernelUserId)
                .kernelPrincipal(kernelPrincipal)
                .isNewRecord(true)
                .build();
        return userRepository.save(Objects.requireNonNull(user))
                .doOnSuccess(saved -> eventPublisher.publishEvent(new AuditEvent("REGISTER_CLIENT", "AUTH",
                        "New client pending email verification: " + saved.getEmail())))
                .flatMap(saved -> KernelContextHolder.current()
                        .flatMap(ctx -> syncClientToKernel(saved, ctx))
                        .thenReturn(saved));
    }

    private Mono<UserEntity> applyClientProfile(UserEntity user, RegisterRequest request, UUID kernelUserId,
                                                String kernelPrincipal) {
        user.setFirstname(request.firstname());
        user.setLastname(request.lastname());
        user.setFullname(request.firstname() + " " + request.lastname());
        user.setRole("CLIENT");
        if (kernelUserId != null) {
            user.setKernelUserId(kernelUserId);
        }
        if (kernelPrincipal != null && !kernelPrincipal.isBlank()) {
            user.setKernelPrincipal(kernelPrincipal);
        }
        return userRepository.save(user)
                .doOnSuccess(saved -> eventPublisher.publishEvent(new AuditEvent("REGISTER_CLIENT", "AUTH",
                        "New client: " + saved.getEmail())))
                .flatMap(saved -> KernelContextHolder.current()
                        .flatMap(ctx -> syncClientToKernel(saved, ctx))
                        .thenReturn(saved));
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
                .flatMap(saved -> syncClientToKernel(saved, KernelRequestContext.empty()).thenReturn(saved))
                .map(u -> new RegisterClientResponse(u, false, CLIENT_REGISTERED_MESSAGE));
    }

    @Override
    // EMAIL_NOT_VERIFIED est un ValidationException utilisé pour flow control
    // (retour d'info à l'UI, pas une vraie erreur). Sans noRollbackFor, la
    // transaction rollback et le pendingUser saved est perdu — ce qui casse
    // ensuite le fix accountType='FREELANCE' du registerFreelance.
    @Transactional(noRollbackFor = ValidationException.class)
    public Mono<OrganizationEntity> registerOrganization(OrgRegisterRequest request) {
        Mono<OrganizationEntity> flow = kernelProperties.isIntegrationEnabled()
                ? kernelRegisterOrganization(request)
                : localRegisterOrganization(request);
        return flow
                .doOnSuccess(org -> {
                    if (org != null) {
                        auditUseCase.record(org.getOwnerId(), "SIGNUP_ORG_SUCCESS", "ORGANIZATION",
                                org.getId(), null, null, null).subscribe();
                    }
                })
                .doOnError(err -> auditUseCase.record(null, "SIGNUP_ORG_FAILED", null, null, null, null,
                        signupFailureMetadata(request.email(), err)).subscribe());
    }

    private Mono<OrganizationEntity> kernelRegisterOrganization(OrgRegisterRequest request) {
        // Pattern Kernel : username strictement [A-Za-z0-9._-] (pas de @).
        // Sans ça, Kernel génère silencieusement un placeholder pending-<uuid>
        // et l'utilisateur ne peut plus se connecter avec son email d'origine.
        String kernelUsername = KernelUsernameGenerator.fromEmail(request.email());

        Map<String, Object> signUpPayload = new HashMap<>();
        signUpPayload.put("tenantId", kernelProperties.getTenantId());
        signUpPayload.put("firstName", request.firstname());
        signUpPayload.put("lastName", request.lastname());
        signUpPayload.put("username", kernelUsername);
        signUpPayload.put("email", request.email());
        signUpPayload.put("password", request.password());
        signUpPayload.put("accountType", "BUSINESS");
        signUpPayload.put("businessType", "RETAIL");

        return kernelAuthAdapter.signUp(signUpPayload)
                .flatMap(signUpData -> {
                    UUID kernelUserId = parseUuid(signUpData.path("id").asText(null));
                    String status = signUpData.path("status").asText(null);
                    // Username effectivement retenu par Kernel (peut différer si collision)
                    String effectivePrincipal = signUpData.path("username").asText(kernelUsername);

                    // Attribuer OWNER au user dès le signup — indispensable pour que
                    // l'onboarding (POST /api/organizations) fonctionne après verif email.
                    Mono<Void> ensureOwner = kernelUserId != null
                            ? kernelOwnerAssignmentService.assignOwnerRole(kernelUserId)
                            : Mono.empty();

                    if ("EMAIL_VERIFICATION_REQUIRED".equals(status)) {
                        // Créer le user local avec les bons firstname/lastname AVANT de retourner
                        // l'erreur — sinon au login suivant, un nouveau user est créé depuis le JWT
                        // qui n'a pas ces champs → fallback sur email.
                        UserEntity pendingUser = UserEntity.builder()
                                .id(UUID.randomUUID())
                                .email(request.email())
                                .firstname(request.firstname())
                                .lastname(request.lastname())
                                .fullname((request.firstname() + " " + request.lastname()).trim())
                                .role("ORGANIZATION")
                                .kernelUserId(kernelUserId)
                                .kernelPrincipal(effectivePrincipal)
                                .hiredAt(java.time.LocalDateTime.now())
                                .isNewRecord(true)
                                .build();
                        return userRepository.findByEmail(request.email())
                                .switchIfEmpty(userRepository.save(pendingUser))
                                .then(ensureOwner)
                                .then(Mono.error(new ValidationException(
                                        "EMAIL_NOT_VERIFIED: Account created. "
                                                + "Check your email to verify before signing in.")));
                    }
                    String token = signUpData.path("accessToken").asText(null);
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
                                                savedUser.setKernelPrincipal(effectivePrincipal);
                                                savedUser.setRole("ORGANIZATION");
                                                // Le sync from token JWT ne récupère pas firstName/lastName
                                                // (pas dans le token) — les setter explicitement depuis la request
                                                savedUser.setFirstname(request.firstname());
                                                savedUser.setLastname(request.lastname());
                                                savedUser.setFullname((request.firstname() + " "
                                                        + request.lastname()).trim());
                                                savedUser.setHiredAt(java.time.LocalDateTime.now());
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

    @Override
    public Mono<OrganizationEntity> registerFreelance(RegisterFreelanceRequest request) {
        String orgName = (request.firstname() + " " + request.lastname()).trim();
        OrgRegisterRequest orgReq = new OrgRegisterRequest(
                request.firstname(),
                request.lastname(),
                request.email(),
                request.password(),
                orgName);

        return registerOrganization(orgReq)
                // Cas nominal (mode local sans vérif email) : org retournée directement.
                .flatMap(org -> markAsFreelance(org, request))
                .flatMap(org -> createDefaultFreelanceAgency(org, request).thenReturn(org))
                // Cas Kernel : registerOrganization jette EMAIL_NOT_VERIFIED avant que l'org
                // soit créée. On mémorise quand même le flag freelance sur le user pending
                // pour que l'org, quand elle sera créée à l'onboarding, hérite bien de
                // accountType='FREELANCE'.
                .onErrorResume(ValidationException.class, ex -> {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "";
                    if (!msg.contains("EMAIL_NOT_VERIFIED")) {
                        return Mono.error(ex);
                    }
                    return userRepository.findByEmail(request.email())
                            .flatMap(user -> {
                                user.setAccountType("FREELANCE");
                                return userRepository.save(user);
                            })
                            // Garantir une org locale FREELANCE + agence dès le signup, même quand
                            // Kernel diffère la création (EMAIL_NOT_VERIFIED). Idempotent via
                            // findByOwnerId : l'org sera réutilisée à l'onboarding, pas dupliquée.
                            .flatMap(user -> orgRepository.findByOwnerId(user.getId())
                                    .switchIfEmpty(Mono.defer(() -> createLocalFreelanceOrg(user, request)))
                                    .onErrorResume(e -> {
                                        log.warn("[freelance] création org locale différée échouée: {}",
                                                e.getMessage());
                                        return Mono.empty();
                                    }))
                            .then(Mono.error(ex));
                })
                .doOnSuccess(o -> {
                    eventPublisher.publishEvent(new AuditEvent(
                            "REGISTER_FREELANCE", "AUTH",
                            "Nouveau freelance: " + o.getName() + " (city=" + request.city() + ")"));
                    auditUseCase.record(o.getOwnerId(), "SIGNUP_FREELANCE_SUCCESS", "ORGANIZATION",
                            o.getId(), null, null, null).subscribe();
                })
                .doOnError(err -> auditUseCase.record(null, "SIGNUP_FREELANCE_FAILED", null, null, null, null,
                        signupFailureMetadata(request.email(), err)).subscribe());
    }

    private Mono<OrganizationEntity> markAsFreelance(OrganizationEntity org, RegisterFreelanceRequest request) {
        org.setAccountType("FREELANCE");
        org.setCity(request.city());
        org.setPhone(request.phone());
        if (request.planId() != null) {
            org.setSubscriptionPlanId(request.planId());
        }
        return orgRepository.save(org);
    }

    /**
     * Crée une organisation locale FREELANCE + agence par défaut sans dépendre de Kernel
     * (kernelOrganizationId reste null, à lier plus tard). Utilisé quand Kernel diffère la
     * création d'org (EMAIL_NOT_VERIFIED) pour ne jamais laisser un freelance sans org.
     */
    private Mono<OrganizationEntity> createLocalFreelanceOrg(UserEntity user, RegisterFreelanceRequest request) {
        String orgName = (request.firstname() + " " + request.lastname()).trim();
        return planRepository.findByName("FREE")
                .switchIfEmpty(Mono.error(new RuntimeException("Plan FREE non configuré en base")))
                .flatMap(freePlan -> {
                    OrganizationEntity org = OrganizationEntity.builder()
                            .id(UUID.randomUUID())
                            .name(orgName.isBlank() ? user.getEmail() : orgName)
                            .ownerId(user.getId())
                            .email(user.getEmail())
                            .country("CM")
                            .timezone("Africa/Douala")
                            .accountType("FREELANCE")
                            .city(request.city())
                            .phone(request.phone())
                            .subscriptionPlanId(request.planId() != null ? request.planId() : freePlan.getId())
                            .subscriptionAutoRenew(true)
                            .isVerified(false)
                            .isDriverBookingRequired(false)
                            .isNewRecord(true)
                            .build();
                    return orgRepository.save(Objects.requireNonNull(org))
                            .flatMap(savedOrg -> subscriptionService
                                    .createHistoryRecord(savedOrg.getId(), freePlan.getName(), null)
                                    .then(createDefaultFreelanceAgency(savedOrg, request))
                                    .thenReturn(savedOrg));
                });
    }

    private Mono<AgencyEntity> createDefaultFreelanceAgency(OrganizationEntity org, RegisterFreelanceRequest request) {
        String city = request.city() != null && !request.city().isBlank() ? request.city() : "Principale";
        // Freelance : l'agence porte le nom du freelance (org et agence représentent la même personne)
        String freelanceName = (request.firstname() + " " + request.lastname()).trim();
        AgencyEntity agency = AgencyEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(org.getId())
                .name(freelanceName)
                .address(city)
                .city(city)
                .country("CM")
                .phone(request.phone())
                .email(request.email())
                .managerId(org.getOwnerId())
                .isNewRecord(true)
                .build();
        return agencyRepository.save(Objects.requireNonNull(agency));
    }

    @Override
    public Mono<Void> requestEmailVerification() {
        if (!kernelProperties.isIntegrationEnabled()) {
            return Mono.empty();
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(email -> {
                    String kernelToken = kernelSessionStore.resolve(email)
                            .orElseThrow(() -> new com.yowyob.easyrental.shared.exception.ValidationException(
                                    "SESSION_REQUIRED: Connectez-vous avant de demander la vérification email."));
                    KernelRequestContext ctx = KernelRequestContext.builder()
                            .bearerToken(java.util.Optional.of(kernelToken))
                            .build();
                    return kernelAuthAdapter.requestEmailVerification(ctx);
                })
                .then();
    }

    @Override
    public Mono<Void> confirmEmailVerification(String verificationToken) {
        if (!kernelProperties.isIntegrationEnabled()) {
            return Mono.empty();
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(email -> {
                    String kernelToken = kernelSessionStore.resolve(email)
                            .orElseThrow(() -> new com.yowyob.easyrental.shared.exception.ValidationException(
                                    "SESSION_REQUIRED: Connectez-vous avant de confirmer la vérification email."));
                    KernelRequestContext ctx = KernelRequestContext.builder()
                            .bearerToken(java.util.Optional.of(kernelToken))
                            .build();
                    return kernelAuthAdapter.confirmEmailVerification(verificationToken, ctx);
                })
                .then();
    }

    private Mono<Void> syncClientToKernel(UserEntity savedUser, KernelRequestContext ctx) {
        if (!kernelProperties.isIntegrationEnabled()) {
            return Mono.empty();
        }
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("firstName", savedUser.getFirstname() != null ? savedUser.getFirstname() : "");
        payload.put("lastName", savedUser.getLastname() != null ? savedUser.getLastname() : "");
        payload.put("email", savedUser.getEmail());
        payload.put("thirdPartyType", "CLIENT");
        return Mono.defer(() -> kernelTpAdapter.createClient(payload, ctx))
                .flatMap(kernelData -> {
                    String id = kernelData.path("id").asText(null);
                    if (id != null) {
                        try {
                            savedUser.setKernelClientId(UUID.fromString(id));
                            return userRepository.save(savedUser).then();
                        } catch (IllegalArgumentException ex) {
                            return Mono.empty();
                        }
                    }
                    return Mono.empty();
                })
                .onErrorResume(ex -> {
                    log.warn("Kernel tp-core sync failed (best-effort): {}", ex.getMessage());
                    return Mono.empty();
                });
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }
}
