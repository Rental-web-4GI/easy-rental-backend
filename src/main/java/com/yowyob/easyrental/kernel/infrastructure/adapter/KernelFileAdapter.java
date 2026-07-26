package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.application.KernelAppTokenProvider;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

/**
 * Adapter for file upload operations against kernel file-core.
 *
 * @author Easy Rental Team
 * @since 2026-07-15
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KernelFileAdapter {

    private final @Qualifier("kernelFileWebClient") WebClient kernelFileWebClient;
    private final KernelClientProperties kernelProperties;
    private final KernelAppTokenProvider appTokenProvider;

    public record KernelFileResult(String fileId, String url, String filename) {}

    public Mono<KernelFileResult> upload(FilePart filePart, KernelRequestContext context) {
        MediaType contentType = filePart.headers().getContentType() != null
                ? filePart.headers().getContentType()
                : MediaType.APPLICATION_OCTET_STREAM;
        String filename = filePart.filename();

        return DataBufferUtils.join(filePart.content())
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return bytes;
                })
                .flatMap(bytes -> sendMultipart(bytes, filename, contentType, context));
    }

    private Mono<KernelFileResult> sendMultipart(byte[] bytes,
                                                 String filename,
                                                 MediaType contentType,
                                                 KernelRequestContext context) {
        int maxBytes = kernelProperties.getFileUploadMaxBytes();
        if (bytes.length > maxBytes) {
            log.warn("[kernel-file] rejected {} ({} bytes > limit {})", filename, bytes.length, maxBytes);
            return Mono.error(new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Fichier trop volumineux (" + (bytes.length / 1024 / 1024)
                            + " Mo). Limite : " + (maxBytes / 1024 / 1024) + " Mo."));
        }
        log.info("[kernel-file] uploading {} ({} bytes)", filename, bytes.length);

        // File-core est un appel MACHINE (X-Client-Id/X-Api-Key + token app).
        // On force un token app frais (le kernel invalide l'ancien sur nouvelle session),
        // et on retente une fois sur 401 avec un token re-rafraîchi.
        return appTokenProvider.freshToken()
                .flatMap(freshOpt -> doUpload(bytes, filename, contentType,
                        freshOpt.or(context::bearerToken).orElse(null)))
                .onErrorResume(err -> err.getMessage() != null && err.getMessage().contains("401")
                        ? appTokenProvider.freshToken()
                                .flatMap(freshOpt -> doUpload(bytes, filename, contentType,
                                        freshOpt.or(context::bearerToken).orElse(null)))
                        : Mono.error(err));
    }

    private Mono<KernelFileResult> doUpload(byte[] bytes, String filename,
                                            MediaType contentType, String bearer) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        }).contentType(contentType);

        return kernelFileWebClient.post()
                .uri("/api/files")
                .headers(headers -> {
                    applyMachineHeaders(headers);
                    if (bearer != null) {
                        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
                    }
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(raw -> {
                            log.info("[kernel-file] status={} body={}", response.statusCode(), raw);
                            if (!response.statusCode().is2xxSuccessful()) {
                                return Mono.error(new RuntimeException(
                                        "Kernel file upload failed: " + response.statusCode() + " " + raw));
                            }
                            if (raw.isEmpty()) {
                                return Mono.error(new RuntimeException("Kernel returned empty body"));
                            }
                            try {
                                JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw);
                                return Mono.just(node);
                            } catch (Exception e) {
                                return Mono.error(new RuntimeException("Kernel returned non-JSON: " + raw, e));
                            }
                        })
                        .flatMap(KernelResponseSupport::unwrapData)
                        .map(data -> {
                            String id = data.path("id").asText(null);
                            String url = data.path("url").asText(null);
                            if (url == null && id != null) {
                                String base = kernelProperties.getBaseUrl();
                                if (base.endsWith("/")) {
                                    base = base.substring(0, base.length() - 1);
                                }
                                url = base + "/api/files/" + id + "/content";
                            }
                            String fallbackName = data.path("filename").asText(filename);
                            String name = data.path("fileName").asText(fallbackName);
                            return new KernelFileResult(id, url, name);
                        }));
    }

    private void applyMachineHeaders(HttpHeaders headers) {
        headers.set("X-Client-Id", kernelProperties.getClientId());
        headers.set("X-Api-Key", kernelProperties.getApiKey());
        headers.set("X-Tenant-Id", kernelProperties.getTenantId());
    }
}
