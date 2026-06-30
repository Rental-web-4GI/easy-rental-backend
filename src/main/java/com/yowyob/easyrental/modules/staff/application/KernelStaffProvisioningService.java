package com.yowyob.easyrental.modules.staff.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelAdministrationAdapter;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelAuthAdapter;
import com.yowyob.easyrental.kernel.security.KernelJwtValidator;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.staff.domain.StaffOnboardingCredentials;
import com.yowyob.easyrental.modules.staff.domain.port.out.StaffOnboardingEmailPort;
import com.yowyob.easyrental.modules.staff.dto.StaffInviteRequestDTO;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Orchestrates kernel account creation and employee invitation for staff provisioning.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class KernelStaffProvisioningService {

    private static final Duration KERNEL_LOGIN_TIMEOUT = Duration.ofSeconds(30);
    private static final int KERNEL_LOGIN_RETRIES = 2;

    private final KernelAuthAdapter kernelAuthAdapter;
    private final KernelAdministrationAdapter kernelAdministrationAdapter;
    private final KernelClientProperties kernelProperties;
    private final KernelJwtValidator kernelJwtValidator;
    private final StaffOnboardingEmailPort staffOnboardingEmailPort;
    private final EasyRentalProperties easyRentalProperties;

    public record ProvisionResult(UUID kernelUserId, boolean newAccount, String temporaryPassword) {
    }

    public Mono<ProvisionResult> provisionKernelUser(
            StaffInviteRequestDTO request,
            Function<String, Mono<UserEntity>> findByEmail) {
        String email = normalizeEmail(request.email());
        return findByEmail.apply(email)
                .flatMap(this::resolveExistingUser)
                .switchIfEmpty(Mono.defer(() -> createKernelAccount(request, email, findByEmail)));
    }

    public Mono<JsonNode> inviteEmployee(
            UUID kernelOrganizationId,
            UUID kernelAgencyId,
            UUID kernelUserId,
            UUID kernelRoleId,
            KernelRequestContext ctx) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", kernelUserId.toString());
        payload.put("roleId", kernelRoleId.toString());
        payload.put("agencyId", kernelAgencyId.toString());
        return kernelAdministrationAdapter.inviteEmployee(kernelOrganizationId, payload, ctx);
    }

    public Mono<Void> sendOnboardingEmail(StaffInviteRequestDTO request, ProvisionResult provisionResult) {
        String password = provisionResult.newAccount() && provisionResult.temporaryPassword() != null
                ? provisionResult.temporaryPassword()
                : "(utilisez votre mot de passe existant)";
        return staffOnboardingEmailPort.sendCredentials(new StaffOnboardingCredentials(
                request.email(),
                password,
                easyRentalProperties.getAgencyLoginUrl(),
                request.firstname(),
                request.lastname()));
    }

    private Mono<ProvisionResult> resolveExistingUser(UserEntity user) {
        if (user.getKernelUserId() == null) {
            return Mono.error(new ValidationException(
                    "USER_NOT_LINKED: Cet utilisateur doit se connecter une fois sur Easy Rental "
                            + "avant d'être invité comme agent."));
        }
        if ("ORGANIZATION".equalsIgnoreCase(user.getRole())) {
            return Mono.error(new ValidationException(
                    "CANNOT_INVITE_OWNER: Cet email appartient au propriétaire de l'organisation."));
        }
        return Mono.just(new ProvisionResult(user.getKernelUserId(), false, null));
    }

    private Mono<ProvisionResult> createKernelAccount(
            StaffInviteRequestDTO request,
            String email,
            Function<String, Mono<UserEntity>> findByEmail) {
        String temporaryPassword = TemporaryPasswordGenerator.generate();
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", kernelProperties.getTenantId());
        payload.put("firstName", request.firstname());
        payload.put("lastName", request.lastname());
        payload.put("username", email);
        payload.put("email", email);
        payload.put("password", temporaryPassword);
        payload.put("accountType", "BUSINESS");
        payload.put("businessType", "RETAIL");

        return kernelAuthAdapter.signUp(payload)
                .flatMap(signUpData -> resolveKernelUserIdAfterSignup(email, temporaryPassword, signUpData)
                        .map(kernelUserId -> new ProvisionResult(kernelUserId, true, temporaryPassword))
                        .defaultIfEmpty(new ProvisionResult(null, true, temporaryPassword)))
                .onErrorResume(ex -> {
                    if (isDuplicateUsername(ex)) {
                        return findByEmail.apply(email)
                                .flatMap(local -> {
                                    if (local.getPassword() != null && local.getKernelUserId() == null) {
                                        return Mono.just(new ProvisionResult(null, false, null));
                                    }
                                    return Mono.error(new ValidationException(
                                            "USER_ALREADY_EXISTS: Un compte existe déjà pour cet email. "
                                                    + "Utilisez un autre email."));
                                })
                                .switchIfEmpty(Mono.error(new ValidationException(
                                        "USER_ALREADY_EXISTS: Un compte existe déjà pour cet email. "
                                                + "Utilisez un autre email.")));
                    }
                    return Mono.error(ex);
                });
    }

    private Mono<UUID> resolveKernelUserIdAfterSignup(String email, String password, JsonNode signUpData) {
        UUID fromResponse = parseUuid(KernelResponseSupport.textOrNull(signUpData, "id"));
        if (fromResponse == null) {
            fromResponse = parseUuid(signUpData.path("userId").asText(null));
        }
        if (fromResponse != null) {
            return Mono.just(fromResponse);
        }
        return attemptKernelLogin(email, password, KERNEL_LOGIN_RETRIES)
                .flatMap(result -> {
                    if (result.mfaRequired()) {
                        return Mono.error(new ValidationException(
                                "KERNEL_MFA_REQUIRED: Le compte créé nécessite une validation MFA. "
                                        + "Réessayez après configuration du compte."));
                    }
                    return kernelJwtValidator.validate(result.accessToken())
                            .map(claims -> UUID.fromString(claims.subject()));
                })
                .onErrorResume(ex -> {
                    if (ex instanceof ValidationException validationException) {
                        return Mono.error(validationException);
                    }
                    return Mono.empty();
                });
    }

    private Mono<com.yowyob.easyrental.kernel.infrastructure.dto.KernelLoginResult> attemptKernelLogin(
            String email, String password, int retriesLeft) {
        return kernelAuthAdapter.login(email, password)
                .timeout(KERNEL_LOGIN_TIMEOUT)
                .onErrorResume(ex -> retriesLeft > 0
                        ? Mono.delay(Duration.ofSeconds(2))
                                .then(attemptKernelLogin(email, password, retriesLeft - 1))
                        : Mono.error(ex));
    }

    private static boolean isDuplicateUsername(Throwable ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        return message.contains("USERNAME_DUPLICATE") || message.contains("already exists");
    }

    private static String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
