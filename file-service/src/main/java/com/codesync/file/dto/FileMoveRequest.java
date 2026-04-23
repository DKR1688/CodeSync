package com.codesync.file.dto;

import jakarta.validation.constraints.NotBlank;

public class FileMoveRequest {

	@NotBlank(message = "New path is required")
	private String newPath;

	public String getNewPath() {
		return newPath;
	}

	public void setNewPath(String newPath) {
		this.newPath = newPath;
	}
}
