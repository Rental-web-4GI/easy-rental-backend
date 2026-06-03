package com.yowyob.easyrental.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "7KyhCtwGUvIoqcxTCabQGm3FJ/F6LfCy9nmOg3hy+WA=";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtUtil.generateToken("user@test.com", "STAFF");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("user@test.com", jwtUtil.getUsernameFromToken(token));
        assertEquals("STAFF", jwtUtil.getRoleFromToken(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.value"));
    }

    @Test
    void shouldExtractClaimFromToken() {
        String token = jwtUtil.generateToken("admin@test.com", "ORGANIZATION");

        assertEquals("admin@test.com", jwtUtil.extractClaim(token, claims -> claims.getSubject()));
    }
}
