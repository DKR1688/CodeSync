package com.codesync.version.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class CreateSnapshotRequest {

	@NotNull
	@Positive
	private Long projectId;

	@NotNull
	@Positive
	private Long fileId;

	private Long parentSnapshotId;

	@NotNull
	private String content;

	@NotBlank
	private String message;

	private String branch;

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

	public Long getParentSnapshotId() {
		return parentSnapshotId;
	}

	public void setParentSnapshotId(Long parentSnapshotId) {
		this.parentSnapshotId = parentSnapshotId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}
}
