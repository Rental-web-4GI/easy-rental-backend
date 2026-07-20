package com.yowyob.easyrental.kernel.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelAdministrationAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Attribue le rôle OWNER (scope TENANT) à un utilisateur nouvellement inscrit,
 * en utilisant le token app-level (platform admin) obtenu via
 * {@link KernelAppTokenProvider}. Sans ce rôle, {@code POST /api/organizations}
 * retourne 403 côté kernel.
 *
 * <p>L'identifiant du rôle OWNER est mis en cache après la première résolution
 * (il ne change quasi jamais).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KernelOwnerAssignmentService {

    private final KernelAdministrationAdapter administrationAdapter;
    private final KernelAppTokenProvider appTokenProvider;

    private volatile UUID cachedOwnerRoleId;

    /**
     * Attribue le rôle OWNER (scope TENANT) à un utilisateur.
     * <p>Se termine silencieusement (empty) en cas d'échec pour ne pas
     * bloquer le flow d'inscription — un log warn est émis.</p>
     */
    public Mono<Void> assignOwnerRole(UUID userId) {
        Optional<String> appToken = appTokenProvider.currentToken();
        if (appToken.isEmpty()) {
            log.warn("[owner-assign] app token unavailable — cannot assign OWNER to user {}", userId);
            return Mono.empty();
        }
        KernelRequestContext adminContext = KernelRequestContext.builder()
                .bearerToken(appToken)
                .build();

        return resolveOwnerRoleId(adminContext)
                .flatMap(roleId -> {
                    Map<String, Object> payload = Map.of(
                            "roleId", roleId.toString(),
                            "scopeType", "TENANT",
                            "scope", "TENANT");
                    return administrationAdapter.assignRole(userId, payload, adminContext)
                            .doOnSuccess(node -> log.info(
                                    "[owner-assign] OWNER attribué à user {} (roleId={})", userId, roleId))
                            .then();
                })
                .onErrorResume(ex -> {
                    log.warn("[owner-assign] échec attribution OWNER user {} : {}", userId, ex.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<UUID> resolveOwnerRoleId(KernelRequestContext adminContext) {
        UUID cached = cachedOwnerRoleId;
        if (cached != null) {
            return Mono.just(cached);
        }
        return administrationAdapter.listRoles(adminContext)
                .filter(this::isOwnerRole)
                .next()
                .flatMap(this::extractRoleId)
                .doOnNext(id -> cachedOwnerRoleId = id)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("[owner-assign] rôle OWNER introuvable dans /api/administration/roles");
                    return Mono.empty();
                }));
    }

    private boolean isOwnerRole(JsonNode role) {
        if (role == null) {
            return false;
        }
        String code = role.path("code").asText("").toUpperCase();
        String name = role.path("name").asText("").toUpperCase();
        return code.equals("OWNER") || name.equals("OWNER");
    }

    private Mono<UUID> extractRoleId(JsonNode role) {
        String id = role.path("id").asText(null);
        if (id == null || id.isBlank()) {
            return Mono.empty();
        }
        try {
            return Mono.just(UUID.fromString(id));
        } catch (IllegalArgumentException ex) {
            log.warn("[owner-assign] roleId non-UUID: {}", id);
            return Mono.empty();
        }
    }
}
