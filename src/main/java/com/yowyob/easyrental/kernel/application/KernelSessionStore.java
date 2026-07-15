package com.yowyob.easyrental.kernel.application;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores short-lived kernel access tokens (and optional refresh tokens) keyed by user email
 * for local JWT sessions.
 *
 * @author Easy Rental Team
 * @since 2026-06-29
 */
@Component
public class KernelSessionStore {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(14);

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();

    public void store(String email, String kernelAccessToken) {
        store(email, kernelAccessToken, DEFAULT_TTL);
    }

    public void store(String email, String kernelAccessToken, Duration ttl) {
        if (email == null || email.isBlank() || kernelAccessToken == null || kernelAccessToken.isBlank()) {
            return;
        }
        String key = normalize(email);
        String existingRefresh = Optional.ofNullable(sessions.get(key))
                .map(SessionEntry::refreshToken)
                .orElse(null);
        sessions.put(key, new SessionEntry(kernelAccessToken, existingRefresh, Instant.now().plus(ttl)));
    }

    public void storeWithRefresh(String email, String accessToken, String refreshToken) {
        if (email == null || email.isBlank() || accessToken == null || accessToken.isBlank()) {
            return;
        }
        sessions.put(normalize(email),
                new SessionEntry(accessToken, refreshToken, Instant.now().plus(DEFAULT_TTL)));
    }

    public Optional<String> resolve(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String key = normalize(email);
        SessionEntry entry = sessions.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            sessions.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.kernelAccessToken());
    }

    public Optional<String> resolveRefreshToken(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(normalize(email)))
                .map(SessionEntry::refreshToken);
    }

    public void evict(String email) {
        if (email != null && !email.isBlank()) {
            sessions.remove(normalize(email));
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private record SessionEntry(String kernelAccessToken, String refreshToken, Instant expiresAt) {
    }
}
