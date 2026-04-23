package com.codesync.execution.dto;

import com.codesync.execution.enums.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class ExecutionResultDTO {

	private UUID jobId;
	private ExecutionStatus status;
	private String stdout;
	private String stderr;
	private Integer exitCode;
	private Long executionTimeMs;
	private Long memoryUsedKb;
	private LocalDateTime completedAt;

	public ExecutionResultDTO() {
	}

	public ExecutionResultDTO(UUID jobId, ExecutionStatus status, String stdout, String stderr, Integer exitCode,
			Long executionTimeMs, Long memoryUsedKb, LocalDateTime completedAt) {
		this.jobId = jobId;
		this.status = status;
		this.stdout = stdout;
		this.stderr = stderr;
		this.exitCode = exitCode;
		this.executionTimeMs = executionTimeMs;
		this.memoryUsedKb = memoryUsedKb;
		this.completedAt = completedAt;
	}

	public UUID getJobId() {
		return jobId;
	}

	public void setJobId(UUID jobId) {
		this.jobId = jobId;
	}

	public ExecutionStatus getStatus() {
		return status;
	}

	public void setStatus(ExecutionStatus status) {
		this.status = status;
	}

	public String getStdout() {
		return stdout;
	}

	public void setStdout(String stdout) {
		this.stdout = stdout;
	}

	public String getStderr() {
		return stderr;
	}

	public void setStderr(String stderr) {
		this.stderr = stderr;
	}

	public Integer getExitCode() {
		return exitCode;
	}

	public void setExitCode(Integer exitCode) {
		this.exitCode = exitCode;
	}

	public Long getExecutionTimeMs() {
		return executionTimeMs;
	}

	public void setExecutionTimeMs(Long executionTimeMs) {
		this.executionTimeMs = executionTimeMs;
	}

	public Long getMemoryUsedKb() {
		return memoryUsedKb;
	}

	public void setMemoryUsedKb(Long memoryUsedKb) {
		this.memoryUsedKb = memoryUsedKb;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}
}
