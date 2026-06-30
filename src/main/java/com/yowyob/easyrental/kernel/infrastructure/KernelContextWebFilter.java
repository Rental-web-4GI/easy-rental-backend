package com.yowyob.easyrental.kernel.infrastructure;

import com.yowyob.easyrental.kernel.application.KernelSessionStore;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.shared.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Optional;
import java.util.UUID;

/**
 * Captures bearer and org/agency headers for kernel delegation.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
public class KernelContextWebFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final KernelSessionStore kernelSessionStore;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        Optional<String> bearer = Optional.empty();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            bearer = Optional.of(authHeader.substring(7));
        }

        Optional<UUID> orgId = parseUuid(exchange.getRequest().getHeaders().getFirst("X-Organization-Id"));
        Optional<UUID> agencyId = parseUuid(exchange.getRequest().getHeaders().getFirst("X-Agency-Id"));

        KernelRequestContext kernelContext = KernelRequestContext.builder()
                .bearerToken(resolveKernelBearer(bearer))
                .organizationId(orgId)
                .agencyId(agencyId)
                .build();

        return chain.filter(exchange)
                .contextWrite(Context.of(KernelContextHolder.CONTEXT_KEY, kernelContext));
    }

    private Optional<String> resolveKernelBearer(Optional<String> bearer) {
        if (bearer.isEmpty()) {
            return bearer;
        }
        String token = bearer.get();
        if (jwtUtil.validateToken(token)) {
            String email = jwtUtil.getUsernameFromToken(token);
            return kernelSessionStore.resolve(email).or(() -> bearer);
        }
        return bearer;
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
