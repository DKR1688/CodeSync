package com.codesync.version.dto;

public class RestoreSnapshotRequest {

	private String branch;
	private String message;

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
