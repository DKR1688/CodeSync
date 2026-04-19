package com.codesync.auth.security;

public record AuthenticatedUser(int userId, String email, String role) {

	public boolean isAdmin() {
		return "ADMIN".equalsIgnoreCase(role);
	}
}
