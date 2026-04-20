package com.codesync.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateFolderRequest {

	@NotNull(message = "Project id is required")
	@Positive(message = "Project id must be greater than 0")
	private Long projectId;

	@NotBlank(message = "Path is required")
	private String path;

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
