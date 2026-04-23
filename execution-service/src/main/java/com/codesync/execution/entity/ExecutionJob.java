package com.codesync.execution.entity;

import com.codesync.execution.enums.ExecutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "execution_jobs", indexes = {
		@Index(name = "idx_execution_project", columnList = "project_id"),
		@Index(name = "idx_execution_file", columnList = "file_id"),
		@Index(name = "idx_execution_user", columnList = "user_id"),
		@Index(name = "idx_execution_status", columnList = "status"),
		@Index(name = "idx_execution_language", columnList = "language"),
		@Index(name = "idx_execution_created_at", columnList = "created_at")
})
public class ExecutionJob {

	@Id
	@Column(name = "job_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID jobId;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "file_id")
	private Long fileId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 50)
	private String language;

	@Column(name = "source_code", nullable = false, columnDefinition = "LONGTEXT")
	private String sourceCode = "";

	@Column(columnDefinition = "TEXT")
	private String stdin = "";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ExecutionStatus status = ExecutionStatus.QUEUED;

	@Column(columnDefinition = "LONGTEXT")
	private String stdout = "";

	@Column(columnDefinition = "LONGTEXT")
	private String stderr = "";

	@Column(name = "exit_code")
	private Integer exitCode;

	@Column(name = "execution_time_ms")
	private Long executionTimeMs;

	@Column(name = "memory_used_kb")
	private Long memoryUsedKb;

	@Column(name = "time_limit_seconds", nullable = false)
	private Integer timeLimitSeconds = 10;

	@Column(name = "memory_limit_mb", nullable = false)
	private Integer memoryLimitMb = 256;

	@Column(name = "cpu_limit", nullable = false)
	private Double cpuLimit = 1.0;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@PrePersist
	public void onCreate() {
		if (jobId == null) {
			jobId = UUID.randomUUID();
		}
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (status == null) {
			status = ExecutionStatus.QUEUED;
		}
	}

	public UUID getJobId() {
		return jobId;
	}

	public void setJobId(UUID jobId) {
		this.jobId = jobId;
	}

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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(LocalDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ExecutionJob other)) {
			return false;
		}
		return Objects.equals(jobId, other.jobId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(jobId);
	}
}
