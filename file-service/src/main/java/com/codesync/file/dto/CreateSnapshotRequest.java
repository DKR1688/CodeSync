package com.codesync.file.dto;

public class CreateSnapshotRequest {

	private Long projectId;
	private Long fileId;
	private Long parentSnapshotId;
	private String content;
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
