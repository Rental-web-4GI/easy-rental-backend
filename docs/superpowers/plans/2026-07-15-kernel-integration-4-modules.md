# Kernel Integration — 4 Modules Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Brancher Easy Rental Backend sur 4 services kernel manquants : token refresh, vérification email, upload fichiers (file-core), et clients comme tiers (tp-core).

**Architecture:** Chaque intégration suit le pattern existant — un adapter kernel (`kernel/infrastructure/adapter/`) appelle le kernel via `KernelHttpPort`, et le use case métier l'invoque si `kernelProperties.isIntegrationEnabled() == true`, avec fallback local si nécessaire. Aucune modification du contrat HTTP exposé au frontend sauf ajout des deux endpoints email-verification.

**Tech Stack:** Java 21, Spring Boot 3.4, WebFlux/Reactor, R2DBC PostgreSQL, Liquibase, JUnit 5 + Mockito

## Global Constraints

- `kernel.base-url` = `https://kernel-core.yowyob.com` — ne pas modifier
- Tous les appels kernel passent par `KernelHttpPort` (jamais `WebClient` directement dans un use case)
- Headers machine obligatoires : `X-Client-Id`, `X-Api-Key`, `X-Tenant-Id` — gérés par `KernelWebClientAdapter`
- Headers org quand scope organisationnel : `X-Organization-Id` — via `KernelRequestContext.organizationId`
- Fallback local systématique si kernel désactivé ou erreur réseau (pattern `onErrorResume` existant)
- Tests avec Mockito — pas de vraie base de données ni vrai appel kernel dans les tests unitaires
- Liquibase pour toute migration de schéma — fichier dans `src/main/resources/db/changelog/`
- `kernel.integration.enabled=false` en local par défaut — les tests du fallback doivent fonctionner sans kernel

---

## Task 1 — Token Refresh via Kernel

**Objectif :** Quand le token local Easy Rental est valide mais que le token kernel stocké dans `KernelSessionStore` est expiré (TTL 14 min), appeler `POST /api/auth/refresh` pour obtenir un nouveau token kernel et le re-stocker. Actuellement `refreshToken()` régénère seulement un JWT local sans toucher le kernel.

**Files:**
- Modify: `src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelAuthAdapter.java`
- Modify: `src/main/java/com/yowyob/easyrental/kernel/application/KernelSessionStore.java`
- Modify: `src/main/java/com/yowyob/easyrental/modules/auth/application/AuthUseCaseImpl.java`
- Test: `src/test/java/com/yowyob/easyrental/modules/auth/application/AuthUseCaseImplTest.java`

**Interfaces:**
- Produit: `KernelAuthAdapter.refresh(String kernelAccessToken): Mono<KernelLoginResult>`
- Produit: `KernelSessionStore.storeWithRefresh(String email, String accessToken, String refreshToken)`
- Produit: `KernelSessionStore.resolveRefreshToken(String email): Optional<String>`

---

- [ ] **Étape 1 : Ajouter `refresh()` dans `KernelAuthAdapter`**

Dans `src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelAuthAdapter.java`, ajouter après la méthode `confirmMfa()` :

```java
public Mono<KernelLoginResult> refresh(String kernelAccessToken) {
    return kernelWebClient.post()
            .uri("/api/auth/refresh")
            .headers(this::applyMachineHeaders)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + kernelAccessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of())
            .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                    .flatMap(body -> KernelResponseSupport.unwrapData(body)
                            .map(data -> {
                                String newToken = resolveAccessToken(data);
                                if (newToken == null) {
                                    throw new com.yowyob.easyrental.shared.exception.ValidationException(
                                            "KERNEL_REFRESH_FAILED: Le refresh du token kernel a échoué.");
                                }
                                return KernelLoginResult.authenticated(newToken);
                            })));
}
```

- [ ] **Étape 2 : Étendre `KernelSessionStore` pour stocker le refresh token**

Remplacer le contenu de `KernelSessionStore.java` :

```java
package com.yowyob.easyrental.kernel.application;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class KernelSessionStore {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(14);

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    public void store(String email, String kernelAccessToken) {
        store(email, kernelAccessToken, DEFAULT_TTL);
    }

    public void store(String email, String kernelAccessToken, Duration ttl) {
        if (email == null || email.isBlank() || kernelAccessToken == null || kernelAccessToken.isBlank()) {
            return;
        }
        String key = normalize(email);
        String existingRefresh = Optional.ofNullable(sessions.get(key))
                .map(SessionEntry::refreshToken)
                .orElse(null);
        sessions.put(key, new SessionEntry(kernelAccessToken, existingRefresh, Instant.now().plus(ttl)));
    }

    public void storeWithRefresh(String email, String accessToken, String refreshToken) {
        if (email == null || email.isBlank() || accessToken == null || accessToken.isBlank()) {
            return;
        }
        sessions.put(normalize(email),
                new SessionEntry(accessToken, refreshToken, Instant.now().plus(DEFAULT_TTL)));
    }

    public Optional<String> resolve(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String key = normalize(email);
        SessionEntry entry = sessions.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            sessions.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.kernelAccessToken());
    }

    public Optional<String> resolveRefreshToken(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(normalize(email)))
                .map(SessionEntry::refreshToken);
    }

    public void evict(String email) {
        if (email != null && !email.isBlank()) {
            sessions.remove(normalize(email));
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private record SessionEntry(String kernelAccessToken, String refreshToken, Instant expiresAt) {
    }
}
```

- [ ] **Étape 3 : Mettre à jour `refreshToken()` dans `AuthUseCaseImpl`**

Remplacer la méthode `refreshToken` existante (ligne ~170) :

```java
@Override
public Mono<AuthResponse> refreshToken(String oldToken) {
    if (oldToken != null && oldToken.startsWith("Bearer ")) {
        oldToken = oldToken.substring(7);
    }
    if (jwtUtil.validateToken(oldToken)) {
        String email = jwtUtil.getUsernameFromToken(oldToken);
        String freshLocalToken = userRepository.findByEmail(email)
                .map(user -> jwtUtil.generateToken(user.getEmail(), user.getRole()))
                .block(); // utilisé seulement pour construire la réponse — OK en sync ici
        if (kernelProperties.isIntegrationEnabled()) {
            final String finalEmail = email;
            return kernelSessionStore.resolve(finalEmail)
                    .map(Mono::just)
                    .orElseGet(() -> kernelSessionStore.resolveRefreshToken(finalEmail)
                            .map(refreshToken -> kernelAuthAdapter.refresh(refreshToken)
                                    .doOnSuccess(result ->
                                            kernelSessionStore.store(finalEmail, result.accessToken()))
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
```

- [ ] **Étape 4 : Écrire le test unitaire**

Dans `AuthUseCaseImplTest.java`, ajouter :

```java
@Test
void refreshToken_whenLocalTokenValid_andKernelEnabled_andSessionExpired_refreshesKernelToken() {
    // Arrange
    String email = "owner@test.com";
    String validLocalToken = jwtUtil.generateToken(email, "ORGANIZATION");
    UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).role("ORGANIZATION").build();

    when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
    when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));
    // kernel session expirée (resolve retourne empty)
    // refresh token disponible
    when(kernelSessionStore.resolve(email)).thenReturn(Optional.empty());
    when(kernelSessionStore.resolveRefreshToken(email)).thenReturn(Optional.of("old-refresh-token"));
    when(kernelAuthAdapter.refresh("old-refresh-token"))
            .thenReturn(Mono.just(KernelLoginResult.authenticated("new-kernel-token")));

    // Act
    AuthResponse response = authUseCase.refreshToken("Bearer " + validLocalToken).block();

    // Assert
    assertNotNull(response);
    assertNotNull(response.token());
    verify(kernelSessionStore).store(email, "new-kernel-token");
}

@Test
void refreshToken_whenLocalTokenInvalid_throwsValidationException() {
    // Act & Assert
    StepVerifier.create(authUseCase.refreshToken("Bearer invalid.token.here"))
            .expectErrorMatches(ex -> ex instanceof ValidationException
                    && ex.getMessage().contains("SESSION_EXPIRED"))
            .verify();
}
```

- [ ] **Étape 5 : Lancer les tests**

```bash
cd easy-rental-backend
./mvnw test -pl . -Dtest=AuthUseCaseImplTest -q
```

Résultat attendu : les deux nouveaux tests passent, aucun test existant ne régresse.

- [ ] **Étape 6 : Commit**

```bash
git add src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelAuthAdapter.java \
        src/main/java/com/yowyob/easyrental/kernel/application/KernelSessionStore.java \
        src/main/java/com/yowyob/easyrental/modules/auth/application/AuthUseCaseImpl.java \
        src/test/java/com/yowyob/easyrental/modules/auth/application/AuthUseCaseImplTest.java
git commit -m "feat: kernel token refresh via /api/auth/refresh with session store"
```

---

## Task 2 — Email Verification via Kernel

**Objectif :** Exposer deux nouveaux endpoints Easy Rental (`POST /api/auth/email-verification/request` et `POST /api/auth/email-verification/confirm`) qui relaient vers le kernel. Appelés après inscription client en prod (`skip-email-verification=false`).

**Files:**
- Modify: `src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelAuthAdapter.java`
- Modify: `src/main/java/com/yowyob/easyrental/modules/auth/domain/port/in/AuthUseCase.java`
- Modify: `src/main/java/com/yowyob/easyrental/modules/auth/application/AuthUseCaseImpl.java`
- Modify: `src/main/java/com/yowyob/easyrental/modules/auth/infrastructure/adapter/in/web/AuthController.java`
- Create: `src/main/java/com/yowyob/easyrental/modules/auth/dto/EmailVerificationConfirmRequest.java`

**Interfaces:**
- Produit: `KernelAuthAdapter.requestEmailVerification(KernelRequestContext ctx): Mono<JsonNode>`
- Produit: `KernelAuthAdapter.confirmEmailVerification(String token, KernelRequestContext ctx): Mono<JsonNode>`
- Produit: `AuthUseCase.requestEmailVerification(): Mono<Void>`
- Produit: `AuthUseCase.confirmEmailVerification(String token): Mono<Void>`

---

- [ ] **Étape 1 : Créer le DTO de confirmation**

Créer `src/main/java/com/yowyob/easyrental/modules/auth/dto/EmailVerificationConfirmRequest.java` :

```java
package com.yowyob.easyrental.modules.auth.dto;

public record EmailVerificationConfirmRequest(String verificationToken) {}
```

- [ ] **Étape 2 : Ajouter les méthodes dans `KernelAuthAdapter`**

Ajouter dans `KernelAuthAdapter.java` :

```java
public Mono<JsonNode> requestEmailVerification(KernelRequestContext context) {
    return kernelWebClient.post()
            .uri("/api/auth/email-verification/request")
            .headers(this::applyMachineHeaders)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + context.bearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of())
            .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                    .flatMap(KernelResponseSupport::unwrapData));
}

public Mono<JsonNode> confirmEmailVerification(String verificationToken, KernelRequestContext context) {
    return kernelWebClient.post()
            .uri("/api/auth/email-verification/confirm")
            .headers(this::applyMachineHeaders)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + context.bearerToken())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("verificationToken", verificationToken))
            .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                    .flatMap(KernelResponseSupport::unwrapData));
}
```

- [ ] **Étape 3 : Ajouter les méthodes dans `AuthUseCase` (port)**

Dans `AuthUseCase.java`, ajouter :

```java
Mono<Void> requestEmailVerification();
Mono<Void> confirmEmailVerification(String verificationToken);
```

- [ ] **Étape 4 : Implémenter dans `AuthUseCaseImpl`**

Ajouter dans `AuthUseCaseImpl.java` :

```java
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
                KernelRequestContext ctx = KernelRequestContext.of(kernelToken);
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
                KernelRequestContext ctx = KernelRequestContext.of(kernelToken);
                return kernelAuthAdapter.confirmEmailVerification(verificationToken, ctx);
            })
            .then();
}
```

Vérifier que `KernelRequestContext` a une méthode statique `of(String bearerToken)`. Si non, utiliser :
```java
KernelRequestContext ctx = KernelRequestContext.builder().bearerToken(kernelToken).build();
```

- [ ] **Étape 5 : Ajouter les endpoints dans `AuthController`**

Dans `AuthController.java`, ajouter après le endpoint `/refresh` :

```java
@PostMapping("/email-verification/request")
public Mono<ResponseEntity<Void>> requestEmailVerification() {
    return authUseCase.requestEmailVerification()
            .thenReturn(ResponseEntity.<Void>accepted().build());
}

@PostMapping("/email-verification/confirm")
public Mono<ResponseEntity<Void>> confirmEmailVerification(
        @RequestBody EmailVerificationConfirmRequest request) {
    return authUseCase.confirmEmailVerification(request.verificationToken())
            .thenReturn(ResponseEntity.<Void>ok().build());
}
```

Ajouter l'import : `import com.yowyob.easyrental.modules.auth.dto.EmailVerificationConfirmRequest;`

- [ ] **Étape 6 : Vérifier que ces routes sont accessibles sans OWNER scope**

Dans `SecurityConfig.java`, vérifier que `/api/auth/email-verification/**` est dans la liste des routes `permitAll` ou au minimum accessible avec un JWT standard. Si non, ajouter.

- [ ] **Étape 7 : Écrire le test unitaire**

Dans `AuthUseCaseImplTest.java`, ajouter :

```java
@Test
void requestEmailVerification_whenKernelEnabled_callsKernelAdapter() {
    String email = "client@test.com";
    String kernelToken = "kernel-access-token";

    when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
    when(kernelSessionStore.resolve(email)).thenReturn(Optional.of(kernelToken));
    when(kernelAuthAdapter.requestEmailVerification(any(KernelRequestContext.class)))
            .thenReturn(Mono.just(mock(com.fasterxml.jackson.databind.JsonNode.class)));

    // Simuler SecurityContext avec email
    // (utiliser WithMockUser ou SecurityContext mock selon le setup existant du test)
    StepVerifier.create(authUseCase.requestEmailVerification())
            .verifyComplete();

    verify(kernelAuthAdapter).requestEmailVerification(any(KernelRequestContext.class));
}

@Test
void requestEmailVerification_whenKernelDisabled_completesImmediately() {
    when(kernelProperties.isIntegrationEnabled()).thenReturn(false);

    StepVerifier.create(authUseCase.requestEmailVerification())
            .verifyComplete();

    verifyNoInteractions(kernelAuthAdapter);
}
```

- [ ] **Étape 8 : Lancer les tests**

```bash
./mvnw test -pl . -Dtest=AuthUseCaseImplTest -q
```

Résultat attendu : PASS.

- [ ] **Étape 9 : Commit**

```bash
git add src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelAuthAdapter.java \
        src/main/java/com/yowyob/easyrental/modules/auth/domain/port/in/AuthUseCase.java \
        src/main/java/com/yowyob/easyrental/modules/auth/application/AuthUseCaseImpl.java \
        src/main/java/com/yowyob/easyrental/modules/auth/infrastructure/adapter/in/web/AuthController.java \
        src/main/java/com/yowyob/easyrental/modules/auth/dto/EmailVerificationConfirmRequest.java
git commit -m "feat: email verification relay to kernel /api/auth/email-verification"
```

---

## Task 3 — File-Core Upload via Kernel

**Objectif :** Quand le kernel est activé, envoyer les fichiers uploadés vers `POST /api/files` du kernel (file-core) au lieu de les stocker localement sur disque. Conserver le fallback local si kernel désactivé ou erreur. Stocker l'URL kernel retournée dans `MediaEntity`.

**Files:**
- Create: `src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelFileAdapter.java`
- Modify: `src/main/java/com/yowyob/easyrental/modules/media/application/MediaUseCaseImpl.java`
- Modify: `src/main/java/com/yowyob/easyrental/kernel/config/KernelWebClientConfig.java`
- Test: `src/test/java/com/yowyob/easyrental/modules/media/application/MediaUseCaseImplTest.java`

**Interfaces:**
- Produit: `KernelFileAdapter.upload(FilePart file, KernelRequestContext ctx): Mono<KernelFileResult>`
- Produit: `record KernelFileResult(String fileId, String url, String filename)`

---

- [ ] **Étape 1 : Créer `KernelFileAdapter`**

Créer `src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelFileAdapter.java` :

```java
package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KernelFileAdapter {

    private final WebClient kernelWebClient;

    public record KernelFileResult(String fileId, String url, String filename) {}

    public Mono<KernelFileResult> upload(FilePart filePart, KernelRequestContext context) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.asyncPart("file", filePart.content(), DataBuffer.class)
                .filename(filePart.filename())
                .contentType(filePart.headers().getContentType() != null
                        ? filePart.headers().getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM);

        return kernelWebClient.post()
                .uri("/api/files")
                .headers(headers -> {
                    headers.set("X-Tenant-Id", context.tenantId() != null ? context.tenantId() : "");
                    if (context.bearerToken() != null) {
                        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + context.bearerToken());
                    }
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchangeToMono(response -> response.bodyToMono(JsonNode.class)
                        .flatMap(KernelResponseSupport::unwrapData)
                        .map(data -> new KernelFileResult(
                                data.path("id").asText(null),
                                data.path("url").asText(null),
                                data.path("filename").asText(filePart.filename())
                        )));
    }
}
```

- [ ] **Étape 2 : Injecter `KernelFileAdapter` et `KernelClientProperties` dans `MediaUseCaseImpl`**

En haut de `MediaUseCaseImpl.java`, ajouter les injections :

```java
private final KernelFileAdapter kernelFileAdapter;
private final KernelClientProperties kernelProperties;
```

Ajouter les imports nécessaires :
```java
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelFileAdapter;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
```

- [ ] **Étape 3 : Modifier `uploadFile()` pour brancher le kernel**

Remplacer la méthode `uploadFile` dans `MediaUseCaseImpl.java` :

```java
public Mono<MediaEntity> uploadFile(FilePart filePart) {
    return validateUpload(filePart)
            .then(resolveCurrentUser())
            .flatMap(user -> {
                if (kernelProperties.isIntegrationEnabled()) {
                    return uploadViaKernel(filePart, user);
                }
                return uploadLocal(filePart, user);
            });
}

private Mono<MediaEntity> uploadViaKernel(FilePart filePart, UserEntity user) {
    return KernelContextHolder.current()
            .flatMap(ctx -> kernelFileAdapter.upload(filePart, ctx)
                    .flatMap(result -> {
                        MediaEntity media = MediaEntity.builder()
                                .id(UUID.randomUUID())
                                .filename(result.filename())
                                .url(result.url() != null ? result.url() : result.fileId())
                                .uploadedBy(user.getId())
                                .uploadedAt(LocalDateTime.now())
                                .isNewRecord(true)
                                .build();
                        return mediaRepository.save(media);
                    }))
            .onErrorResume(ex -> {
                log.warn("Kernel file upload failed, falling back to local: {}", ex.getMessage());
                return uploadLocal(filePart, user);
            });
}

private Mono<MediaEntity> uploadLocal(FilePart filePart, UserEntity user) {
    Mono<String> prefixMono;
    if ("ORGANIZATION".equals(user.getRole())) {
        prefixMono = organizationRepository.findByOwnerId(user.getId())
                .map(org -> sanitizeFilename(org.getName()))
                .defaultIfEmpty("org_" + user.getId());
    } else if ("STAFF".equals(user.getRole()) && user.getOrganizationId() != null) {
        prefixMono = organizationRepository.findById(user.getOrganizationId())
                .map(org -> sanitizeFilename(org.getName()))
                .defaultIfEmpty("staff_" + user.getId());
    } else {
        prefixMono = Mono.just("user_" + sanitizeFilename(user.getLastname()));
    }

    return prefixMono.flatMap(prefix -> {
        String extension = getFileExtension(filePart.filename());
        String uniqueName = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path destinationFile = Paths.get(uploadDir).resolve(uniqueName).toAbsolutePath();
        String publicUrl = baseUrl + "/uploads/" + uniqueName;

        return filePart.transferTo(destinationFile)
                .then(saveMediaEntity(filePart, uniqueName, publicUrl, user.getId()));
    });
}
```

- [ ] **Étape 4 : Vérifier que `KernelRequestContext` expose le tenantId**

Dans `KernelFileAdapter.upload()`, la ligne `context.tenantId()` doit compiler. Si `KernelRequestContext` n'a pas de méthode `tenantId()`, utiliser `kernelProperties.getTenantId()` injecté dans l'adapter à la place.

- [ ] **Étape 5 : Écrire le test unitaire**

Dans `MediaUseCaseImplTest.java`, ajouter :

```java
@Test
void uploadFile_whenKernelEnabled_uploadsToKernel() {
    FilePart filePart = mock(FilePart.class);
    HttpHeaders partHeaders = new HttpHeaders();
    partHeaders.setContentType(MediaType.IMAGE_JPEG);
    when(filePart.filename()).thenReturn("photo.jpg");
    when(filePart.headers()).thenReturn(partHeaders);
    when(filePart.content()).thenReturn(Flux.empty());

    UserEntity user = UserEntity.builder().id(UUID.randomUUID()).role("CLIENT")
            .lastname("Dupont").build();
    KernelFileAdapter.KernelFileResult kernelResult =
            new KernelFileAdapter.KernelFileResult("file-uuid-123",
                    "https://kernel-core.yowyob.com/api/files/file-uuid-123/content",
                    "photo.jpg");

    when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
    when(kernelFileAdapter.upload(eq(filePart), any(KernelRequestContext.class)))
            .thenReturn(Mono.just(kernelResult));
    when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    MediaEntity result = mediaUseCase.uploadFile(filePart).block();

    assertNotNull(result);
    assertEquals("https://kernel-core.yowyob.com/api/files/file-uuid-123/content", result.getUrl());
}

@Test
void uploadFile_whenKernelFails_fallsBackToLocal() {
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn("doc.pdf");
    when(filePart.headers()).thenReturn(new HttpHeaders());

    when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
    when(kernelFileAdapter.upload(any(), any())).thenReturn(Mono.error(new RuntimeException("Timeout")));
    // setup local upload mock...

    // Vérifie que le fallback local est appelé sans exception
    StepVerifier.create(mediaUseCase.uploadFile(filePart))
            .expectNextCount(1)
            .verifyComplete();
}
```

- [ ] **Étape 6 : Lancer les tests**

```bash
./mvnw test -pl . -Dtest=MediaUseCaseImplTest -q
```

Résultat attendu : PASS.

- [ ] **Étape 7 : Commit**

```bash
git add src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelFileAdapter.java \
        src/main/java/com/yowyob/easyrental/modules/media/application/MediaUseCaseImpl.java
git commit -m "feat: file upload via kernel file-core with local fallback"
```

---

## Task 4 — Clients comme Tiers (tp-core)

**Objectif :** Après inscription d'un client Easy Rental, le déclarer dans le kernel comme tiers (`POST /api/clients`) et stocker son `kernelClientId` sur `UserEntity`. Permet au kernel de gérer le profil commercial du client (CRM, scoring, suivi). Le client existant reste opérationnel sans kernel (champ nullable).

**Files:**
- Create: `src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelTpAdapter.java`
- Modify: `src/main/java/com/yowyob/easyrental/modules/auth/domain/UserEntity.java`
- Create: `src/main/resources/db/changelog/V_add_kernel_client_id.sql` (ou `.yaml` selon convention du projet)
- Modify: `src/main/java/com/yowyob/easyrental/modules/auth/application/AuthUseCaseImpl.java`
- Test: `src/test/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelTpAdapterTest.java`

**Interfaces:**
- Produit: `KernelTpAdapter.createClient(Map<String, Object> payload, KernelRequestContext ctx): Mono<JsonNode>`
- Produit: `KernelTpAdapter.getClient(UUID kernelClientId, KernelRequestContext ctx): Mono<JsonNode>`

---

- [ ] **Étape 1 : Vérifier le format de migration Liquibase existant**

```bash
ls easy-rental-backend/src/main/resources/db/changelog/
```

Observer le format utilisé (`.sql`, `.yaml`, `.xml`) et la convention de nommage (ex: `V10__...sql` ou `001_...yaml`). Adapter le nom du fichier de migration en conséquence.

- [ ] **Étape 2 : Créer la migration Liquibase pour `kernel_client_id`**

Créer le fichier de migration (adapter le nom à la convention du projet) :

```sql
-- Exemple pour un fichier .sql Liquibase
-- liquibase formatted sql
-- changeset easy-rental:add-kernel-client-id-to-users

ALTER TABLE users ADD COLUMN IF NOT EXISTS kernel_client_id UUID;
```

Si le projet utilise YAML :
```yaml
databaseChangeLog:
  - changeSet:
      id: add-kernel-client-id-to-users
      author: easy-rental
      changes:
        - addColumn:
            tableName: users
            columns:
              - column:
                  name: kernel_client_id
                  type: UUID
                  constraints:
                    nullable: true
```

- [ ] **Étape 3 : Ajouter `kernelClientId` sur `UserEntity`**

Dans `UserEntity.java`, ajouter après `kernelActorId` :

```java
private UUID kernelClientId;
```

- [ ] **Étape 4 : Créer `KernelTpAdapter`**

Créer `src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelTpAdapter.java` :

```java
package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.domain.port.out.KernelHttpPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KernelTpAdapter {

    private final KernelHttpPort kernelHttpPort;

    public Mono<JsonNode> createClient(Map<String, Object> payload, KernelRequestContext context) {
        return kernelHttpPort.post("/api/clients", payload, context);
    }

    public Mono<JsonNode> getClient(UUID kernelClientId, KernelRequestContext context) {
        return kernelHttpPort.get("/api/clients/" + kernelClientId, context);
    }
}
```

- [ ] **Étape 5 : Appeler `KernelTpAdapter` lors de l'inscription client**

Dans `AuthUseCaseImpl.java`, injecter `KernelTpAdapter` et `UserRepositoryPort` (déjà présent).

Ajouter le champ :
```java
private final KernelTpAdapter kernelTpAdapter;
```

Trouver la méthode `kernelRegisterClient` (ou `localRegisterClient`) et après la sauvegarde locale du user, enchaîner la création du tiers kernel :

```java
private Mono<Void> syncClientToKernel(UserEntity savedUser, KernelRequestContext ctx) {
    if (!kernelProperties.isIntegrationEnabled()) {
        return Mono.empty();
    }
    Map<String, Object> payload = Map.of(
            "firstName", savedUser.getFirstname() != null ? savedUser.getFirstname() : "",
            "lastName", savedUser.getLastname() != null ? savedUser.getLastname() : "",
            "email", savedUser.getEmail(),
            "thirdPartyType", "CLIENT"
    );
    return kernelTpAdapter.createClient(payload, ctx)
            .flatMap(kernelData -> {
                String id = kernelData.path("id").asText(null);
                if (id != null) {
                    savedUser.setKernelClientId(UUID.fromString(id));
                    return userRepository.save(savedUser).then();
                }
                return Mono.empty();
            })
            .onErrorResume(ex -> {
                // Non bloquant : la création kernel du tiers est best-effort
                return Mono.empty();
            });
}
```

Dans `localRegisterClient`, après `userRepository.save(user)`, enchaîner :
```java
.flatMap(saved -> syncClientToKernel(saved, KernelRequestContext.empty())
        .thenReturn(saved))
```

Pour `kernelRegisterClient`, utiliser le context kernel courant :
```java
.flatMap(saved -> KernelContextHolder.current()
        .flatMap(ctx -> syncClientToKernel(saved, ctx))
        .thenReturn(saved))
```

- [ ] **Étape 6 : Écrire le test unitaire**

Créer `src/test/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelTpAdapterTest.java` :

```java
package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.domain.port.out.KernelHttpPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KernelTpAdapterTest {

    @Mock
    private KernelHttpPort kernelHttpPort;

    private KernelTpAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KernelTpAdapter(kernelHttpPort);
    }

    @Test
    void createClient_callsKernelHttpPortWithCorrectPath() throws Exception {
        JsonNode fakeResponse = new ObjectMapper().readTree("{\"id\":\"abc-123\",\"email\":\"client@test.com\"}");
        when(kernelHttpPort.post(eq("/api/clients"), anyMap(), any(KernelRequestContext.class)))
                .thenReturn(Mono.just(fakeResponse));

        Map<String, Object> payload = Map.of("email", "client@test.com", "thirdPartyType", "CLIENT");

        StepVerifier.create(adapter.createClient(payload, KernelRequestContext.empty()))
                .expectNextMatches(node -> "abc-123".equals(node.path("id").asText()))
                .verifyComplete();

        verify(kernelHttpPort).post(eq("/api/clients"), eq(payload), any(KernelRequestContext.class));
    }
}
```

- [ ] **Étape 7 : Lancer les tests**

```bash
./mvnw test -pl . -Dtest=KernelTpAdapterTest -q
```

Résultat attendu : PASS.

- [ ] **Étape 8 : Vérifier la migration au démarrage**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local 2>&1 | grep -E "liquibase|kernel_client_id|ERROR" | head -20
```

Résultat attendu : migration appliquée sans erreur, colonne présente dans la table `users`.

- [ ] **Étape 9 : Commit**

```bash
git add src/main/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelTpAdapter.java \
        src/main/java/com/yowyob/easyrental/modules/auth/domain/UserEntity.java \
        src/main/resources/db/changelog/ \
        src/main/java/com/yowyob/easyrental/modules/auth/application/AuthUseCaseImpl.java \
        src/test/java/com/yowyob/easyrental/kernel/infrastructure/adapter/KernelTpAdapterTest.java
git commit -m "feat: sync clients to kernel tp-core on registration"
```

---

## Self-Review

### Couverture spec

| Objectif | Tâche |
|---|---|
| Token refresh kernel | Task 1 ✅ |
| Email verification (request + confirm) | Task 2 ✅ |
| File upload kernel | Task 3 ✅ |
| Clients comme tiers kernel | Task 4 ✅ |
| Fallback local pour chaque intégration | Tasks 1, 3, 4 ✅ |
| Aucune modification du contrat HTTP frontend (sauf email-verif) | ✅ |
| Tests unitaires pour chaque tâche | ✅ |
| Migration Liquibase pour kernel_client_id | Task 4 ✅ |

### Risques identifiés

- **Task 3** : `KernelRequestContext.tenantId()` peut ne pas exister — vérifier à la compilation et adapter
- **Task 4** : Le payload `POST /api/clients` peut nécessiter des champs supplémentaires (`organizationId`, `businessActorId`) — à valider avec le kernel en test d'intégration
- **Task 2** : `SecurityContextHolder` en contexte réactif — utiliser `ReactiveSecurityContextHolder` (déjà présent dans `AuthUseCaseImpl`)
- **Tous** : S'assurer que `KernelRequestContext.empty()` existe — sinon utiliser `KernelRequestContext.builder().build()`
