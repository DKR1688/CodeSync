package com.codesync.execution.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SubmitExecutionRequest {

	@NotNull
	@Min(1)
	private Long projectId;

	@Min(1)
	private Long fileId;

	@NotBlank
	@Size(max = 50)
	private String language;

	@NotNull
	private String sourceCode;

	private String stdin;

	@Min(1)
	private Integer timeLimitSeconds;

	@Min(16)
	private Integer memoryLimitMb;

	@DecimalMin("0.1")
	@DecimalMax("4.0")
	private Double cpuLimit;

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

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getSourceCode() {
		return sourceCode;
	}

	public void setSourceCode(String sourceCode) {
		this.sourceCode = sourceCode;
	}

	public String getStdin() {
		return stdin;
	}

	public void setStdin(String stdin) {
		this.stdin = stdin;
	}

	public Integer getTimeLimitSeconds() {
		return timeLimitSeconds;
	}

	public void setTimeLimitSeconds(Integer timeLimitSeconds) {
		this.timeLimitSeconds = timeLimitSeconds;
	}

	public Integer getMemoryLimitMb() {
		return memoryLimitMb;
	}

	public void setMemoryLimitMb(Integer memoryLimitMb) {
		this.memoryLimitMb = memoryLimitMb;
	}

	public Double getCpuLimit() {
		return cpuLimit;
	}

	public void setCpuLimit(Double cpuLimit) {
		this.cpuLimit = cpuLimit;
	}
}
