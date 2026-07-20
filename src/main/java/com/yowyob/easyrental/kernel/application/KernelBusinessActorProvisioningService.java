package com.yowyob.easyrental.kernel.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Assure l'existence + l'approbation d'un profil business actor côté kernel.
 * <p>Nécessaire car {@code /api/auth/sign-up} crée un actorId mais PAS le profil business actor
 * complet. Sans profil approuvé, {@code POST /api/organizations} retourne
 * {@code BUSINESS_ACTOR_NOT_APPROVED}.</p>
 *
 * <p>Flow en 2 étapes :</p>
 * <ol>
 *   <li>Avec le token USER : {@code GET /api/actors/me} → si 404, {@code POST /api/actors/onboarding}
 *       pour créer le profil (statut {@code PENDING_REVIEW})</li>
 *   <li>Avec le token ADMIN app-level : {@code POST /api/administration/governance/business-actors/{id}}
 *       avec {@code action=APPROVE} pour passer en {@code APPROVED}</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KernelBusinessActorProvisioningService {

    private final KernelOrganizationAdapter kernelOrganizationAdapter;
    private final KernelAppTokenProvider appTokenProvider;

    /**
     * Assure que le user courant a un profil business actor approuvé et retourne
     * l'ID de ce profil (à utiliser comme {@code businessActorId} pour créer
     * une organisation). Idempotent.
     *
     * @param userContext contexte avec bearer token du user (celui qui possède l'actorId)
     * @param firstname   utilisé si on doit créer le profil
     * @param lastname    utilisé si on doit créer le profil
     * @param businessId  identifiant business libre (ex "ER-{uuid8}")
     * @return Mono contenant le profileId (UUID) du business actor approuvé ; empty si échec
     */
    public Mono<UUID> ensureApprovedBusinessActor(
            KernelRequestContext userContext, String firstname, String lastname, String businessId) {

        return kernelOrganizationAdapter.getMyBusinessActor(userContext)
                .onErrorResume(ex -> {
                    log.info("[ba-provision] profil business actor absent — création");
                    return createBusinessActorProfile(userContext, firstname, lastname, businessId);
                })
                .flatMap(profile -> approveIfNeeded(profile).thenReturn(profile))
                .flatMap(this::extractProfileId)
                .onErrorResume(ex -> {
                    log.warn("[ba-provision] échec provisionnement business actor : {}", ex.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<UUID> extractProfileId(JsonNode profileWrapper) {
        JsonNode data = profileWrapper.has("data") ? profileWrapper.get("data") : profileWrapper;
        String id = data.path("id").asText(null);
        if (id == null) {
            log.warn("[ba-provision] pas d'id retourné par le profil business actor");
            return Mono.empty();
        }
        try {
            return Mono.just(UUID.fromString(id));
        } catch (IllegalArgumentException ex) {
            log.warn("[ba-provision] profileId non-UUID: {}", id);
            return Mono.empty();
        }
    }

    private Mono<JsonNode> createBusinessActorProfile(
            KernelRequestContext userContext, String firstname, String lastname, String businessId) {
        Map<String, Object> payload = new HashMap<>();
        String name = ((firstname != null ? firstname : "") + " " + (lastname != null ? lastname : "")).trim();
        String bId = businessId != null ? businessId : "ER-" + UUID.randomUUID().toString().substring(0, 8);
        payload.put("name", name);
        payload.put("businessId", bId);
        payload.put("role", "OWNER");
        payload.put("type", "BUSINESS");
        payload.put("isIndividual", true);
        payload.put("isActive", true);
        return kernelOrganizationAdapter.createBusinessActor(payload, userContext)
                .flatMap(response -> {
                    // response peut être enveloppée {success, data} ou brute
                    JsonNode data = response.has("data") ? response.get("data") : response;
                    return Mono.just(data);
                });
    }

    private Mono<Void> approveIfNeeded(JsonNode profileData) {
        JsonNode data = profileData.has("data") ? profileData.get("data") : profileData;
        String status = data.path("governanceStatus").asText(null);
        if ("APPROVED".equals(status)) {
            log.debug("[ba-provision] business actor déjà APPROVED");
            return Mono.empty();
        }
        String profileIdStr = data.path("id").asText(null);
        if (profileIdStr == null) {
            log.warn("[ba-provision] pas d'id dans le profil, impossible d'approuver");
            return Mono.empty();
        }
        UUID profileId;
        try {
            profileId = UUID.fromString(profileIdStr);
        } catch (IllegalArgumentException ex) {
            log.warn("[ba-provision] profileId non-UUID: {}", profileIdStr);
            return Mono.empty();
        }
        Optional<String> appToken = appTokenProvider.currentToken();
        if (appToken.isEmpty()) {
            log.warn("[ba-provision] app token indisponible — impossible d'approuver le business actor {}", profileId);
            return Mono.empty();
        }
        KernelRequestContext adminContext = KernelRequestContext.builder()
                .bearerToken(appToken)
                .build();
        return kernelOrganizationAdapter
                .governBusinessActor(profileId, "APPROVE", "Auto-approved by easy-rental", adminContext)
                .doOnSuccess(node -> log.info("[ba-provision] business actor {} APPROVED", profileId))
                .then();
    }
}
