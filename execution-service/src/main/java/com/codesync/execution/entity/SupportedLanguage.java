package com.codesync.execution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "supported_languages")
public class SupportedLanguage {

	@Id
	@Column(nullable = false, length = 50)
	private String language;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Column(name = "runtime_version", nullable = false, length = 100)
	private String runtimeVersion;

	@Column(name = "docker_image", nullable = false, length = 200)
	private String dockerImage;

	@Column(name = "source_file_name", nullable = false, length = 100)
	private String sourceFileName;

	@Column(name = "run_command", nullable = false, length = 1000)
	private String runCommand;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "default_time_limit_seconds", nullable = false)
	private Integer defaultTimeLimitSeconds = 10;

	@Column(name = "default_memory_limit_mb", nullable = false)
	private Integer defaultMemoryLimitMb = 256;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = now;
		}
	}

	@PreUpdate
	public void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getRuntimeVersion() {
		return runtimeVersion;
	}

	public void setRuntimeVersion(String runtimeVersion) {
		this.runtimeVersion = runtimeVersion;
	}

	public String getDockerImage() {
		return dockerImage;
	}

	public void setDockerImage(String dockerImage) {
		this.dockerImage = dockerImage;
	}

	public String getSourceFileName() {
		return sourceFileName;
	}

	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}

	public String getRunCommand() {
		return runCommand;
	}

	public void setRunCommand(String runCommand) {
		this.runCommand = runCommand;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Integer getDefaultTimeLimitSeconds() {
		return defaultTimeLimitSeconds;
	}

	public void setDefaultTimeLimitSeconds(Integer defaultTimeLimitSeconds) {
		this.defaultTimeLimitSeconds = defaultTimeLimitSeconds;
	}

	public Integer getDefaultMemoryLimitMb() {
		return defaultMemoryLimitMb;
	}

	public void setDefaultMemoryLimitMb(Integer defaultMemoryLimitMb) {
		this.defaultMemoryLimitMb = defaultMemoryLimitMb;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
