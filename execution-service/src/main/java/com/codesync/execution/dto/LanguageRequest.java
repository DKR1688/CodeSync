package com.codesync.execution.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LanguageRequest {

	@NotBlank
	@Size(max = 50)
	private String language;

	@NotBlank
	@Size(max = 100)
	private String displayName;

	@NotBlank
	@Size(max = 100)
	private String runtimeVersion;

	@NotBlank
	@Size(max = 200)
	private String dockerImage;

	@NotBlank
	@Size(max = 100)
	private String sourceFileName;

	@NotBlank
	@Size(max = 1000)
	private String runCommand;

	@NotNull
	private Boolean enabled;

	@Min(1)
	private Integer defaultTimeLimitSeconds;

	@Min(16)
	private Integer defaultMemoryLimitMb;

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

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
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
}
