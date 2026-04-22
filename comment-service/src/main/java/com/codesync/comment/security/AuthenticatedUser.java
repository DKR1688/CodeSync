package com.codesync.comment.security;

public record AuthenticatedUser(Long userId, String email, String role) {

	public boolean isAdmin() {
		return "ADMIN".equalsIgnoreCase(role);
	}
}
