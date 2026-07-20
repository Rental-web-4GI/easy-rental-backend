package com.yowyob.easyrental.modules.media.infrastructure.adapter.in.web;

import com.yowyob.easyrental.kernel.application.KernelAppTokenProvider;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.modules.media.domain.port.in.MediaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media Management", description = "Upload de fichiers pour Clients et Organisations")
@SecurityRequirement(name = "bearerAuth")
public class MediaController {

    private final MediaUseCase mediaUseCase;
    private final WebClient kernelWebClient;
    private final KernelClientProperties kernelProperties;
    private final KernelAppTokenProvider appTokenProvider;

    @Operation(summary = "Upload d'un fichier (Image, Doc, etc.)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<MediaResponse>> upload(@RequestPart("file") FilePart file) {
        return mediaUseCase.uploadFile(file)
                .map(media -> ResponseEntity.ok(new MediaResponse(media.getFileUrl(), media.getFilename())));
    }

    @Operation(summary = "Proxy pour télécharger un fichier stocké côté kernel file-core")
    @GetMapping("/kernel-file/{fileId}")
    public Mono<ResponseEntity<byte[]>> proxyKernelFile(@PathVariable String fileId) {
        log.info("[proxy-kernel-file] fileId={} tokenPresent={}",
                fileId, appTokenProvider.currentToken().isPresent());
        return kernelWebClient.get()
                .uri("/api/files/" + fileId)
                .headers(headers -> {
                    headers.set("X-Client-Id", kernelProperties.getClientId());
                    headers.set("X-Api-Key", kernelProperties.getApiKey());
                    headers.set("X-Tenant-Id", kernelProperties.getTenantId());
                    appTokenProvider.currentToken()
                            .ifPresent(token -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token));
                })
                .exchangeToMono(response -> response.bodyToMono(byte[].class)
                        .defaultIfEmpty(new byte[0])
                        .map(bytes -> {
                            MediaType ct = response.headers().contentType()
                                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
                            log.info("[proxy-kernel-file] status={} → {} bytes ({})",
                                    response.statusCode(), bytes.length, ct);
                            return ResponseEntity.status(response.statusCode())
                                    .contentType(ct)
                                    .body(bytes);
                        }))
                .doOnError(ex -> log.error("[proxy-kernel-file] error: {}", ex.getMessage(), ex));
    }

    // DTO simple pour la réponse
    public record MediaResponse(String url, String filename) {}
}
