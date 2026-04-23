package com.codesync.file.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CopyProjectFilesRequest {

	@NotNull(message = "Source project id is required")
	@Positive(message = "Source project id must be greater than 0")
	private Long sourceProjectId;

	@NotNull(message = "Target project id is required")
	@Positive(message = "Target project id must be greater than 0")
	private Long targetProjectId;

	public Long getSourceProjectId() {
		return sourceProjectId;
	}

	public void setSourceProjectId(Long sourceProjectId) {
		this.sourceProjectId = sourceProjectId;
	}

	public Long getTargetProjectId() {
		return targetProjectId;
	}

	public void setTargetProjectId(Long targetProjectId) {
		this.targetProjectId = targetProjectId;
	}
}
