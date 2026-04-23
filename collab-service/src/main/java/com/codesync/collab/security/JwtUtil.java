package com.codesync.collab.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secret;

	public boolean validateToken(String token) {
		try {
			getClaims(token);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public String extractEmail(String token) {
		return getClaims(token).getSubject();
	}

	public Long extractUserId(String token) {
		Object claim = getClaims(token).get("userId");
		if (claim instanceof Number number) {
			return number.longValue();
		}
		throw new IllegalStateException("Token does not contain a valid user id");
	}

	public String extractRole(String token) {
		Object claim = getClaims(token).get("role");
		return claim != null ? claim.toString() : "DEVELOPER";
	}

	private Claims getClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(hashSecret(secret));
	}

	private byte[] hashSecret(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-512");
			return digest.digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-512 algorithm not available", ex);
		}
	}
}
