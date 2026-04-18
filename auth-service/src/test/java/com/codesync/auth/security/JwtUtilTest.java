package com.codesync.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secret = "Yp7dTFj9qK8LwZ2sUxN4vCfIp6ReBaQ0mHk2TzGyR1oSvD3c";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        jwtUtil.setSecret(secret);
        jwtUtil.setExpiration(86400000L); // 1 day
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        String token = jwtUtil.generateToken("test@example.com");

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        String token = jwtUtil.generateToken("test@example.com");

        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_ForInvalidToken() {
        boolean isValid = jwtUtil.validateToken("invalid.token");

        assertFalse(isValid);
    }

    @Test
    void extractEmail_ShouldReturnEmail() {
        String token = jwtUtil.generateToken("test@example.com");

        String email = jwtUtil.extractEmail(token);

        assertEquals("test@example.com", email);
    }

    @Test
    void extractExpiration_ShouldReturnFutureDate() {
        String token = jwtUtil.generateToken("test@example.com");

        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void isTokenExpired_ShouldReturnFalse_ForValidToken() {
        String token = jwtUtil.generateToken("test@example.com");

        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertFalse(isExpired);
    }
}