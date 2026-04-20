package com.codesync.file.dto;

import jakarta.validation.constraints.NotBlank;

public class FileRenameRequest {

	@NotBlank(message = "New name is required")
	private String newName;

	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}
}
