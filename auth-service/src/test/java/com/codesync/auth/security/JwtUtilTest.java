package com.codesync.auth.security;

import com.codesync.auth.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

	@Test
	void generatesAndValidatesTokensWithConfiguredSecret() {
		JwtUtil jwtUtil = new JwtUtil("custom-secret-value", 60_000L);
		User user = new User();
		user.setUserId(42);
		user.setEmail("jwt@example.com");
		user.setRole("DEVELOPER");

		String token = jwtUtil.generateToken(user);

		assertTrue(jwtUtil.validateToken(token));
		assertEquals("jwt@example.com", jwtUtil.extractEmail(token));
		assertEquals(42, jwtUtil.extractUserId(token));
		assertEquals("DEVELOPER", jwtUtil.extractRole(token));
	}

	@Test
	void fallsBackToDefaultSecretWhenConfiguredSecretIsBlank() {
		JwtUtil jwtUtil = new JwtUtil("   ", 60_000L);
		User user = new User();
		user.setUserId(7);
		user.setEmail("fallback@example.com");
		user.setRole("ADMIN");

		String token = jwtUtil.generateToken(user);

		assertTrue(jwtUtil.validateToken(token));
		assertEquals(7, jwtUtil.extractUserId(token));
		assertEquals("ADMIN", jwtUtil.extractRole(token));
	}

}
