package com.yowyob.easyrental.modules.media.application;

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

    @Value("${application.file.upload-dir:uploads}")
    private String uploadDir;

    // Utilisé pour construire l'URL complète si besoin, sinon on renvoie le chemin relatif
    @Value("${application.base-url:http://localhost:8080}")
    private String baseUrl;

    private final MediaRepositoryPort mediaRepository;
    private final UserRepositoryPort userRepository;
    private final OrganizationRepositoryPort organizationRepository;

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
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName()) // Récupère l'email
                .flatMap(email -> userRepository.findByEmail(email))
                .flatMap(user -> {
                    // Logique de nommage
                    Mono<String> prefixMono;

                    if ("ORGANIZATION".equals(user.getRole())) {
                        // Si c'est une ORG, on cherche son nom
                        prefixMono = organizationRepository.findByOwnerId(user
                                .getId()) // Méthode à ajouter dans OrgRepo*
                                .map(org -> sanitizeFilename(org.getName()))
                                .defaultIfEmpty("org_" + user.getId());
                    } else {
                        // Si c'est un CLIENT, on utilise son ID ou nom
                        prefixMono = Mono.just("user_" + sanitizeFilename(user.getLastname()));
                    }

                    return prefixMono.flatMap(prefix -> {
                        // Construction du nom unique
                        String extension = getFileExtension(filePart.filename());
                        String uniqueName = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
                        Path destinationFile = Paths.get(uploadDir).resolve(uniqueName).toAbsolutePath();

                        // URL Publique
                        String publicUrl = baseUrl + "/uploads/" + uniqueName;

                        // Sauvegarde physique + Base de données
                        return filePart.transferTo(Objects.requireNonNull(destinationFile))
                                .then(saveMediaEntity(filePart, uniqueName, publicUrl, user.getId()));
                    });
                });
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
