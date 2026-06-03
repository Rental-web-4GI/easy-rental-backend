package com.yowyob.easyrental.modules.media.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.media.domain.port.in.MediaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Tag(name = "Media Management", description = "Upload de fichiers pour Clients et Organisations")
@SecurityRequirement(name = "bearerAuth")
public class MediaController {

    private final MediaUseCase mediaUseCase;

    @Operation(summary = "Upload d'un fichier (Image, Doc, etc.)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<MediaResponse>> upload(@RequestPart("file") FilePart file) {
        return mediaUseCase.uploadFile(file)
                .map(media -> ResponseEntity.ok(new MediaResponse(media.getFileUrl(), media.getFilename())));
    }

    // DTO simple pour la réponse
    public record MediaResponse(String url, String filename) {}
}
