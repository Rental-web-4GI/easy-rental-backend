package com.yowyob.easyrental.kernel.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.domain.KernelAuthClaims;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import com.yowyob.easyrental.kernel.security.KernelJwtValidator;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * Synchronizes local user records with kernel identities.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class KernelUserMappingService {

    private final UserRepositoryPort userRepository;
    private final KernelJwtValidator kernelJwtValidator;

    public Mono<UserEntity> syncFromKernelLogin(String loginEmail, String accessToken) {
        return kernelJwtValidator.validate(accessToken)
                .flatMap(claims -> upsertUser(loginEmail, claims, null));
    }

    public Mono<UserEntity> syncFromAccessToken(String accessToken) {
        return kernelJwtValidator.validate(accessToken)
                .flatMap(claims -> syncFromClaims(claims, accessToken));
    }

    public Mono<UserEntity> syncFromKernelUser(JsonNode kernelUser, String accessToken) {
        return kernelJwtValidator.validate(accessToken)
                .flatMap(claims -> {
                    String email = KernelResponseSupport.textOrNull(kernelUser, "email");
                    if (email == null) {
                        email = claims.principal();
                    }
                    return upsertUser(email, claims, kernelUser);
                });
    }

    public Mono<UserEntity> syncFromClaims(KernelAuthClaims claims, String accessToken) {
        return upsertUser(claims.principal(), claims, null);
    }

    private Mono<UserEntity> upsertUser(String email, KernelAuthClaims claims, JsonNode kernelUser) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(findByKernelUserId(claims))
                .flatMap(existing -> updateExisting(existing, claims, kernelUser, email))
                .switchIfEmpty(Mono.defer(() -> createNew(email, claims, kernelUser)));
    }

    private Mono<UserEntity> findByKernelUserId(KernelAuthClaims claims) {
        if (claims.subject() == null) {
            return Mono.empty();
        }
        try {
            return userRepository.findByKernelUserId(UUID.fromString(claims.subject()));
        } catch (IllegalArgumentException ex) {
            return Mono.empty();
        }
    }

    private Mono<UserEntity> updateExisting(
            UserEntity user, KernelAuthClaims claims, JsonNode kernelUser, String email) {
        String previousRole = user.getRole();
        applyKernelFields(user, claims, kernelUser, email);
        if ("ORGANIZATION".equals(previousRole) && "CLIENT".equals(user.getRole())) {
            user.setRole("ORGANIZATION");
        }
        return userRepository.save(user);
    }

    private Mono<UserEntity> createNew(String email, KernelAuthClaims claims, JsonNode kernelUser) {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstname(readName(kernelUser, "firstName", email))
                .lastname(readName(kernelUser, "lastName", ""))
                .fullname(readName(kernelUser, "firstName", email) + " " + readName(kernelUser, "lastName", ""))
                .role(resolveRole(claims))
                .password(null)
                .isNewRecord(true)
                .build();
        applyKernelFields(user, claims, kernelUser, email);
        return userRepository.save(Objects.requireNonNull(user));
    }

    private void applyKernelFields(
            UserEntity user, KernelAuthClaims claims, JsonNode kernelUser, String email) {
        claims.tenantId();
        claims.subject();
        if (claims.subject() != null) {
            try {
                user.setKernelUserId(UUID.fromString(claims.subject()));
            } catch (IllegalArgumentException ignored) {
                // keep existing mapping
            }
        }
        claims.actorId().ifPresent(user::setKernelActorId);
        user.setRole(resolveRole(claims));
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }
        if (kernelUser != null) {
            String first = KernelResponseSupport.textOrNull(kernelUser, "firstName");
            String last = KernelResponseSupport.textOrNull(kernelUser, "lastName");
            if (first != null) {
                user.setFirstname(first);
            }
            if (last != null) {
                user.setLastname(last);
            }
            if (first != null || last != null) {
                user.setFullname((first != null ? first : "") + " " + (last != null ? last : ""));
            }
        }
    }

    private String resolveRole(KernelAuthClaims claims) {
        if (com.yowyob.easyrental.kernel.security.KernelPermissionMapper.isOrganizationOwner(claims)
                || com.yowyob.easyrental.kernel.security.KernelPermissionMapper.isOrganizationOwnerCandidate(claims)) {
            return "ORGANIZATION";
        }
        if (com.yowyob.easyrental.kernel.security.KernelPermissionMapper.isAgencyStaff(claims)) {
            return "STAFF";
        }
        if (claims.roles().stream().anyMatch(r -> r.equalsIgnoreCase("ADMIN"))) {
            return "ADMIN";
        }
        return "CLIENT";
    }

    private String readName(JsonNode node, String field, String fallback) {
        if (node == null) {
            return fallback;
        }
        String value = KernelResponseSupport.textOrNull(node, field);
        return value != null ? value : fallback;
    }
}
