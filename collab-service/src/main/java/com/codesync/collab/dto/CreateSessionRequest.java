package com.codesync.collab.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateSessionRequest {

	@NotNull(message = "Project id is required")
	private Long projectId;

	@NotNull(message = "File id is required")
	private Long fileId;

	@Min(value = 1, message = "Max participants must be at least 1")
	@Max(value = 100, message = "Max participants must be at most 100")
	private Integer maxParticipants = 10;

	@Size(max = 100, message = "Session password must be at most 100 characters")
	private String password;

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long fileId) {
		this.fileId = fileId;
	}

	public Integer getMaxParticipants() {
		return maxParticipants;
	}

	public void setMaxParticipants(Integer maxParticipants) {
		this.maxParticipants = maxParticipants;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
