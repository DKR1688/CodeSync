package com.codesync.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.codesync.auth.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration}")
	private long expiration;

	private final Map<String, Date> revokedTokens = new ConcurrentHashMap<>();

	private Key getSigningKey() {
		byte[] keyBytes = hashSecret(secret);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	private byte[] hashSecret(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-512");
			return digest.digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-512 algorithm not available", e);
		}
	}

	public String generateToken(User user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("userId", user.getUserId());
		claims.put("role", user.getRole());
		return buildToken(user.getEmail(), claims);
	}

	public String generateToken(String email) {
		return buildToken(email, Map.of());
	}

	public int extractUserId(String token) {
		Object claim = getClaims(token).get("userId");
		if (claim instanceof Number number) {
			return number.intValue();
		}
		throw new IllegalStateException("Token does not contain a valid user id");
	}

	public String extractRole(String token) {
		Object claim = getClaims(token).get("role");
		return claim != null ? claim.toString() : "DEVELOPER";
	}

	private String buildToken(String email, Map<String, Object> claims) {
		return Jwts.builder()
				.setClaims(claims)
				.setSubject(email)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(getSigningKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public String extractEmail(String token) {
		return getClaims(token).getSubject();
	}

	public void revokeToken(String token) {
		Date expiresAt = getClaims(token).getExpiration();
		cleanupRevokedTokens();
		revokedTokens.put(token, expiresAt);
	}

	public boolean validateToken(String token) {
		try {
			cleanupRevokedTokens();
			getClaims(token);
			return !revokedTokens.containsKey(token);
		} catch (Exception e) {
			return false;
		}
	}

	private void cleanupRevokedTokens() {
		Date now = new Date();
		Iterator<Map.Entry<String, Date>> iterator = revokedTokens.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, Date> entry = iterator.next();
			if (entry.getValue() == null || entry.getValue().before(now)) {
				iterator.remove();
			}
		}
	}

	private Claims getClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}
}
