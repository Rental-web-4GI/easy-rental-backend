package com.yowyob.easyrental.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        // Autoriser votre frontend (ex: React sur localhost:3000)
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000",
                                                    "http://localhost:3001",
                                                    "http://localhost:3002",
                                                    "http://localhost:3003",
                                                    "https://pwa-easy-renta.vercel.app",
                                                    "https://pwa-easy-rental-mfe-client.vercel.app",
                                                    "https://pwa-easy-rental-agency.vercel.app",
                                                    "https://pwa-easy-rental-org.vercel.app"));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "x-requested-with"));
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
