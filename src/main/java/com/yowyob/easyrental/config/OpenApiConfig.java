package com.yowyob.easyrental.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Rental Platform")
                        .version("1.0")
                        .description("Documentation de l'API de gestion de location de véhicules."))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("1-Public-Auth")
                .pathsToMatch(
                        "/auth/**",
                        "/api/media/**",
                        "/api/subscriptions/plans"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi clientApi() {
        return GroupedOpenApi.builder()
                .group("2-Espace-Client")
                .pathsToMatch(
                        "/api/vehicles/available",
                        "/api/vehicles/search",
                        "/api/vehicles/agency/*/available",
                        "/api/vehicles/{id}/details",
                        "/api/vehicles/categories/all",
                        "/api/vehicles/drivers/available",
                        "/api/drivers/{id}/details",
                        "/api/agencies/all",
                        "/api/agencies/search",
                        "/api/agencies/{id}/details",
                        "/api/reviews/**",
                        "/api/rentals/init",
                        "/api/rentals/{id}/pay",
                        "/api/rentals/{id}/cancel",
                        "/api/rentals/{id}/end-signal",
                        "/api/rentals/{id}/details",
                        "/api/rentals/client/**",
                        "/api/notifications/client/**",
                        "/api/notifications/{id}/read",
                        "/api/transactions/client/**",
                        "/api/transactions/{id}/details",
                        "/api/users/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi orgApi() {
        return GroupedOpenApi.builder()
                .group("3-Espace-Gestion")
                .pathsToMatch(
                        "/api/org/**",
                        "/api/agencies/**",
                        "/api/subscriptions/**",
                        "/api/staff/**",
                        "/api/postes/**",
                        "/api/permissions/**",
                        "/api/vehicles/**",
                        "/api/drivers/**",
                        "/api/vehicles/categories/**",
                        "/api/rentals/agency/**",
                        "/api/rentals/org/**",
                        "/api/rentals/{id}/start",
                        "/api/rentals/{id}/validate-return",
                        "/api/rentals/{id}/details",
                        "/api/stats/**",
                        "/api/transactions/org/**",
                        "/api/transactions/agency/**",
                        "/api/transactions/{id}/details",
                        "/api/notifications/org/**",
                        "/api/notifications/agency/**",
                        "/api/notifications/{id}/read",
                        "/api/users/**"
                )
                .build();
    }
}
