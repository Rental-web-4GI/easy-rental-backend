package com.yowyob.easyrental.config;

import com.yowyob.easyrental.shared.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;

/**
 * Spring Security configuration with JWT and RBAC.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtAuthenticationFilter jwtFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/**", "/swagger-ui.html", "/webjars/**", "/v3/api-docs/**").permitAll()
                        .pathMatchers(HttpMethod.PUT, "/api/subscriptions/plans/**").hasRole("ADMIN")
                        .pathMatchers(
                            "/api/vehicles/available",
                            "/api/vehicles/search",
                            "/api/vehicles/agency/*/available",
                            "/uploads/**",
                            "/api/subscriptions/plans/**",
                            "/api/vehicles/categories/all",
                            "/api/agencies/all",
                            "/api/agencies/search",
                            "/api/agencies/*/details",
                            "/api/drivers/{id}/details",
                            "/api/vehicles/{id}/details",
                            "/api/vehicles/drivers/available",
                            "/api/reviews/**",
                            "/api/rentals/{id}/details",
                            "/api/rentals/init")
                        .permitAll()
                        .pathMatchers("/api/org/**").hasRole("ORGANIZATION")
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptionHandlingSpec -> exceptionHandlingSpec
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
