package com.codesync.collab.dto;

import com.codesync.collab.enums.ParticipantRole;
import jakarta.validation.constraints.Size;

public class JoinSessionRequest {

	@Size(max = 100, message = "Session password must be at most 100 characters")
	private String password;

	private ParticipantRole role;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public ParticipantRole getRole() {
		return role;
	}

	public void setRole(ParticipantRole role) {
		this.role = role;
	}
}
