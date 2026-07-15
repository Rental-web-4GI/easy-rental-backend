package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
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
import reactor.core.publisher.Mono;

/**
 * Adapter for file upload operations against kernel file-core.
 *
 * @author Easy Rental Team
 * @since 2026-07-15
 */
@Component
@RequiredArgsConstructor
public class KernelFileAdapter {

    private final WebClient kernelWebClient;
    private final KernelClientProperties kernelProperties;

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
                    applyMachineHeaders(headers);
                    context.bearerToken().ifPresent(token ->
                            headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token));
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

    private void applyMachineHeaders(HttpHeaders headers) {
        headers.set("X-Client-Id", kernelProperties.getClientId());
        headers.set("X-Api-Key", kernelProperties.getApiKey());
        headers.set("X-Tenant-Id", kernelProperties.getTenantId());
    }
}
