package com.yowyob.easyrental.kernel.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelAuthClaims;
import com.yowyob.easyrental.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Validates kernel RS256 JWT using JWKS.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
public class KernelJwtValidator {

    private final KernelClientProperties properties;
    private final WebClient kernelJwksWebClient;

    private volatile JWKSet cachedJwkSet;
    private volatile Instant jwksFetchedAt;

    public Mono<KernelAuthClaims> validate(String token) {
        return getJwkSet()
                .flatMap(jwkSet -> Mono.fromCallable(() -> parseAndVerify(token, jwkSet)))
                .onErrorMap(this::mapValidationError);
    }

    private Throwable mapValidationError(Throwable ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        if (message.contains("timed out") || message.contains("Timeout")) {
            return new com.yowyob.easyrental.shared.exception.ValidationException(
                    "KERNEL_TIMEOUT: Connexion au kernel trop lente. Réessayez dans quelques secondes.");
        }
        return new UnauthorizedException("Invalid kernel token: " + message);
    }

    private KernelAuthClaims parseAndVerify(String token, JWKSet jwkSet) throws Exception {
        ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
        JWSKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
        processor.setJWSKeySelector(keySelector);

        JWTClaimsSet claims = processor.process(token, null);
        return mapClaims(claims);
    }

    private KernelAuthClaims mapClaims(JWTClaimsSet claims) throws ParseException {
        String subject = claims.getSubject();
        String principal = claims.getStringClaim("principal");
        if (principal == null || principal.isBlank()) {
            principal = subject;
        }

        Optional<UUID> actorId = parseUuidClaim(claims, "actorId");
        if (actorId.isEmpty()) {
            actorId = parseUuidClaim(claims, "actor");
        }

        return new KernelAuthClaims(
                subject,
                principal,
                parseUuidClaim(claims, "tid"),
                parseUuidClaim(claims, "oid"),
                parseUuidClaim(claims, "aid"),
                actorId,
                readStringListClaim(claims, "permissions"),
                readStringListClaim(claims, "roles"));
    }

    private Optional<UUID> parseUuidClaim(JWTClaimsSet claims, String name) throws ParseException {
        String value = claims.getStringClaim(name);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(value));
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringListClaim(JWTClaimsSet claims, String name) throws ParseException {
        Object raw = claims.getClaim(name);
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return List.of(raw.toString());
    }

    private Mono<JWKSet> getJwkSet() {
        Instant now = Instant.now();
        if (cachedJwkSet != null && jwksFetchedAt != null && jwksFetchedAt.isAfter(now.minusSeconds(300))) {
            return Mono.just(cachedJwkSet);
        }
        return kernelJwksWebClient.get()
                .uri(properties.resolvedJwksUri())
                .retrieve()
                .bodyToMono(String.class)
                .map(body -> {
                    try {
                        cachedJwkSet = JWKSet.parse(body);
                        jwksFetchedAt = Instant.now();
                        return cachedJwkSet;
                    } catch (ParseException ex) {
                        throw new IllegalStateException("Invalid JWKS document", ex);
                    }
                });
    }
}
