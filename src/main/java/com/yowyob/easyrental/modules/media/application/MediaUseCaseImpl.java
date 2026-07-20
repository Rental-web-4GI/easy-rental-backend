package com.yowyob.easyrental.modules.media.application;

import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelAuthClaims;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelFileAdapter;
import com.yowyob.easyrental.kernel.security.KernelAuthenticationToken;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.media.domain.MediaEntity;
import com.yowyob.easyrental.modules.media.domain.port.out.MediaRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import jakarta.annotation.PostConstruct;
import com.yowyob.easyrental.modules.media.domain.port.in.MediaUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.shared.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaUseCaseImpl implements MediaUseCase {

  private static final long DEFAULT_MAX_UPLOAD_BYTES = 16L * 1024L * 1024L;

  @Value("${application.file.upload-dir:uploads}")
  private String uploadDir;

  @Value("${application.file.max-upload-bytes:16777216}")
  private long maxUploadBytes;

  // Utilisé pour construire l'URL complète si besoin, sinon on renvoie le chemin relatif
  @Value("${application.base-url:http://localhost:8080}")
  private String baseUrl;

    private final MediaRepositoryPort mediaRepository;
    private final UserRepositoryPort userRepository;
    private final OrganizationRepositoryPort organizationRepository;
    private final KernelFileAdapter kernelFileAdapter;
    private final KernelClientProperties kernelProperties;

    // Crée le dossier au démarrage si inexistant
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer le dossier d'upload", e);
        }
    }

    public Mono<MediaEntity> uploadFile(FilePart filePart) {
        return validateUpload(filePart)
                .then(resolveCurrentUser())
                .flatMap(user -> {
                    if (kernelProperties.isIntegrationEnabled() && hasKernelMachineCredentials()) {
                        return uploadViaKernel(filePart, user);
                    }
                    if (kernelProperties.isIntegrationEnabled()) {
                        log.warn("Kernel integration enabled but machine credentials missing "
                                + "(X-Client-Id / X-Api-Key) — falling back to local storage.");
                    }
                    return uploadLocal(filePart, user);
                });
    }

    private boolean hasKernelMachineCredentials() {
        return kernelProperties.getClientId() != null
                && !kernelProperties.getClientId().isBlank()
                && kernelProperties.getApiKey() != null
                && !kernelProperties.getApiKey().isBlank();
    }

    private Mono<MediaEntity> uploadViaKernel(FilePart filePart, UserEntity user) {
        return KernelContextHolder.current()
                .flatMap(ctx -> kernelFileAdapter.upload(filePart, ctx)
                        .flatMap(result -> {
                            // Save an internal proxy URL so the browser can display the image
                            // without needing the kernel machine headers.
                            String fileUrl = baseUrl + "/api/media/kernel-file/" + result.fileId();
                            MediaEntity media = MediaEntity.builder()
                                    .id(UUID.randomUUID())
                                    .filename(result.filename())
                                    .originalFilename(filePart.filename())
                                    .fileType(filePart.headers().getContentType() != null
                                            ? filePart.headers().getContentType().toString()
                                            : "application/octet-stream")
                                    .fileUrl(fileUrl)
                                    .uploaderId(user.getId())
                                    .createdAt(java.time.LocalDateTime.now())
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
                    .map(org -> sanitizeFilename(org.getName() != null ? org.getName() : "org"))
                    .defaultIfEmpty("org_" + user.getId());
        } else if ("STAFF".equals(user.getRole()) && user.getOrganizationId() != null) {
            prefixMono = organizationRepository.findById(user.getOrganizationId())
                    .map(org -> sanitizeFilename(org.getName() != null ? org.getName() : "org"))
                    .defaultIfEmpty("staff_" + user.getId());
        } else {
            String lastname = user.getLastname() != null ? user.getLastname() : "user";
            prefixMono = Mono.just("user_" + sanitizeFilename(lastname));
        }

        return prefixMono
                .doOnNext(prefix -> log.info("[uploadLocal] prefix={}", prefix))
                .flatMap(prefix -> {
                    String extension = getFileExtension(filePart.filename());
                    String uniqueName = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
                    Path destinationFile = Paths.get(uploadDir).resolve(uniqueName).toAbsolutePath();
                    String publicUrl = baseUrl + "/uploads/" + uniqueName;
                    log.info("[uploadLocal] writing to {}", destinationFile);

                    return filePart.transferTo(Objects.requireNonNull(destinationFile))
                            .doOnSuccess(v -> log.info("[uploadLocal] transferTo complete for {}", uniqueName))
                            .doOnError(ex -> log.error("[uploadLocal] transferTo failed: {}", ex.getMessage(), ex))
                            .then(saveMediaEntity(filePart, uniqueName, publicUrl, user.getId()))
                            .doOnSuccess(m -> log.info("[uploadLocal] saved id={} url={}", m.getId(), m.getFileUrl()))
                            .doOnError(ex -> log.error("[uploadLocal] save failed: {}", ex.getMessage(), ex));
                });
    }

    private Mono<Void> validateUpload(FilePart filePart) {
        if (filePart == null || filePart.filename() == null || filePart.filename().isBlank()) {
            return Mono.error(new IllegalArgumentException("Uploaded file is required"));
        }

        String filename = filePart.filename().toLowerCase();
        if (!isAllowedDocument(filename)) {
            return Mono.error(new IllegalArgumentException(
                    "Unsupported file type. Allowed: JPG, PNG, WEBP, PDF"));
        }

        long limit = maxUploadBytes > 0 ? maxUploadBytes : DEFAULT_MAX_UPLOAD_BYTES;
        long contentLength = filePart.headers().getContentLength();
        if (contentLength > 0 && contentLength > limit) {
            return Mono.error(new IllegalArgumentException(
                    "File too large. Maximum size is " + (limit / (1024 * 1024)) + " MB"));
        }

        return Mono.empty();
    }

    private boolean isAllowedDocument(String filename) {
        return filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png")
                || filename.endsWith(".webp")
                || filename.endsWith(".pdf");
    }

    private Mono<UserEntity> resolveCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .flatMap(this::findUserFromAuthentication)
                .switchIfEmpty(Mono.error(new UnauthorizedException("Authenticated user not found for media upload")));
    }

    private Mono<UserEntity> findUserFromAuthentication(Authentication auth) {
        if (auth instanceof KernelAuthenticationToken kernelAuth) {
            KernelAuthClaims claims = kernelAuth.getClaims();
            String principal = claims.principal();
            if (principal != null && principal.contains("@")) {
                return userRepository.findByEmail(principal)
                        .switchIfEmpty(findUserByKernelSubject(claims));
            }
            return findUserByKernelSubject(claims);
        }
        return userRepository.findByEmail(auth.getName());
    }

    private Mono<UserEntity> findUserByKernelSubject(KernelAuthClaims claims) {
        if (claims.subject() == null) {
            return Mono.empty();
        }
        try {
            return userRepository.findByKernelUserId(UUID.fromString(claims.subject()));
        } catch (IllegalArgumentException ex) {
            return Mono.empty();
        }
    }

    private Mono<MediaEntity> saveMediaEntity(FilePart filePart, String filename, String url, UUID uploaderId) {
        // 1. On récupère le MediaType dans une variable locale
        org.springframework.http.MediaType contentType = filePart.headers().getContentType();
    
        // 2. On détermine la chaîne de caractère de façon sécurisée
        String fileTypeString = (contentType != null) ? contentType.toString() : "application/octet-stream";
        MediaEntity media = MediaEntity.builder()
                .id(UUID.randomUUID())
                .filename(filename)
                .originalFilename(filePart.filename())
                .fileType(fileTypeString)
                .fileUrl(url)
                .uploaderId(uploaderId)
                .createdAt(LocalDateTime.now())
                .isNewRecord(true)
                .build();

        return mediaRepository.save(Objects.requireNonNull(media));
    }

    // Utilitaires
    private String sanitizeFilename(String input) {
        return input.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        return (lastDotIndex != -1) ? filename.substring(lastDotIndex) : "";
    }
}
