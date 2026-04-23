package com.codesync.execution.security;

public record AuthenticatedUser(Long userId, String email, String role) {

	public boolean isAdmin() {
		return "ADMIN".equalsIgnoreCase(role);
	}
}
