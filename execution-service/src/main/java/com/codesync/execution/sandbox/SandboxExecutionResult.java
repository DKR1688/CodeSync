package com.codesync.execution.sandbox;

import com.codesync.execution.enums.ExecutionStatus;

public class SandboxExecutionResult {

	private final ExecutionStatus status;
	private final String stdout;
	private final String stderr;
	private final Integer exitCode;
	private final long executionTimeMs;
	private final long memoryUsedKb;

	public SandboxExecutionResult(ExecutionStatus status, String stdout, String stderr, Integer exitCode,
			long executionTimeMs, long memoryUsedKb) {
		this.status = status;
		this.stdout = stdout;
		this.stderr = stderr;
		this.exitCode = exitCode;
		this.executionTimeMs = executionTimeMs;
		this.memoryUsedKb = memoryUsedKb;
	}

	public ExecutionStatus getStatus() {
		return status;
	}

	public String getStdout() {
		return stdout;
	}

	public String getStderr() {
		return stderr;
	}

	public Integer getExitCode() {
		return exitCode;
	}

	public long getExecutionTimeMs() {
		return executionTimeMs;
	}

	public long getMemoryUsedKb() {
		return memoryUsedKb;
	}
}
