package com.yowyob.easyrental.modules.media.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Raises multipart limits so driver documents (PDF scans) can be uploaded reliably.
 */
@Configuration
public class MultipartCodecConfig implements WebFluxConfigurer {

    @Value("${application.file.max-upload-bytes:16777216}")
    private long maxUploadBytes;

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize((int) Math.min(maxUploadBytes, Integer.MAX_VALUE));
    }

    @Bean
    MultipartHttpMessageReader multipartHttpMessageReader() {
        DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
        partReader.setMaxHeadersSize(16 * 1024);
        partReader.setMaxDiskUsagePerPart(maxUploadBytes);
        partReader.setMaxParts(32);
        return new MultipartHttpMessageReader(partReader);
    }
}
