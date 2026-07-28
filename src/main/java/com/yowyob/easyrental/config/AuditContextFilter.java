package com.yowyob.easyrental.config;

import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Place l'adresse IP client et le user-agent dans le contexte Reactor,
 * pour que {@code AuditUseCaseImpl.record} puisse les enregistrer même quand
 * l'appelant ne les fournit pas explicitement (ex. LOGIN_*).
 *
 * <p>Clés de contexte : {@code AUDIT_IP}, {@code AUDIT_UA}.</p>
 *
 * @author Easy Rental Team
 * @since 2026-07-27
 */
@Component
@Order(-100)
public class AuditContextFilter implements WebFilter {

    public static final String CTX_IP = "AUDIT_IP";
    public static final String CTX_UA = "AUDIT_UA";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        String ip;
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For peut contenir une liste : la première IP est le client.
            ip = forwarded.split(",")[0].trim();
        } else if (request.getRemoteAddress() != null) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        } else {
            ip = null;
        }
        String ua = request.getHeaders().getFirst("User-Agent");

        return chain.filter(exchange).contextWrite(ctx -> {
            var c = ctx;
            if (ip != null) {
                c = c.put(CTX_IP, ip);
            }
            if (ua != null) {
                c = c.put(CTX_UA, ua);
            }
            return c;
        });
    }
}
