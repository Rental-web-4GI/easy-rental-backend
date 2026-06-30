package com.yowyob.easyrental.shared.security;

import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.security.KernelAuthenticationToken;
import com.yowyob.easyrental.kernel.security.KernelJwtValidator;
import com.yowyob.easyrental.shared.security.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.Objects;

/**
 * JWT authentication filter supporting local HS256 and kernel RS256 tokens.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Component
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final KernelClientProperties kernelProperties;
    private final KernelJwtValidator kernelJwtValidator;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            KernelClientProperties kernelProperties,
            KernelJwtValidator kernelJwtValidator) {
        this.jwtUtil = jwtUtil;
        this.kernelProperties = kernelProperties;
        this.kernelJwtValidator = kernelJwtValidator;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (kernelProperties.isIntegrationEnabled()) {
                return kernelJwtValidator.validate(token)
                        .map(claims -> new KernelAuthenticationToken(claims, token))
                        .flatMap(auth -> chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                        .onErrorResume(ex -> authenticateLocalToken(token, exchange, chain));
            }

            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );

                return Objects.requireNonNull(
                        chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)),
                        "Le filtre ne doit pas retourner une valeur nulle"
                );
            }
        }
        return Objects.requireNonNull(chain.filter(exchange));
    }

    private Mono<Void> authenticateLocalToken(
            String token, ServerWebExchange exchange, WebFilterChain chain) {
        if (!jwtUtil.validateToken(token)) {
            return chain.filter(exchange);
        }
        String username = jwtUtil.getUsernameFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );
        return Objects.requireNonNull(
                chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)),
                "Le filtre ne doit pas retourner une valeur nulle"
        );
    }
}
