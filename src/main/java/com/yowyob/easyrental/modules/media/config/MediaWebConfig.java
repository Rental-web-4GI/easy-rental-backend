package com.yowyob.easyrental.modules.media.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.lang.NonNull;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class MediaWebConfig implements WebFluxConfigurer {

    @Value("${application.file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Expose le dossier local "uploads" via l'URL "/uploads/**"
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath)
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
    }
}
