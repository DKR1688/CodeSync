package com.codesync.version.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateBranchRequest {

	@NotNull
	@Positive
	private Long sourceSnapshotId;

	@NotBlank
	private String branch;

	private String message;

	public Long getSourceSnapshotId() {
		return sourceSnapshotId;
	}

	public void setSourceSnapshotId(Long sourceSnapshotId) {
		this.sourceSnapshotId = sourceSnapshotId;
	}

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
