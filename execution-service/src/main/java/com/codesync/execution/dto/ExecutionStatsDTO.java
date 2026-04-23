package com.codesync.execution.dto;

import com.codesync.execution.enums.ExecutionStatus;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExecutionStatsDTO {

	private long totalExecutions;
	private Map<ExecutionStatus, Long> executionsByStatus = new EnumMap<>(ExecutionStatus.class);
	private Map<String, Long> executionsByLanguage = new LinkedHashMap<>();
	private long runningExecutions;
	private long completedExecutions;
	private long failedExecutions;
	private double averageExecutionTimeMs;

	public long getTotalExecutions() {
		return totalExecutions;
	}

	public void setTotalExecutions(long totalExecutions) {
		this.totalExecutions = totalExecutions;
	}

	public Map<ExecutionStatus, Long> getExecutionsByStatus() {
		return executionsByStatus;
	}

	public void setExecutionsByStatus(Map<ExecutionStatus, Long> executionsByStatus) {
		this.executionsByStatus = executionsByStatus;
	}

	public Map<String, Long> getExecutionsByLanguage() {
		return executionsByLanguage;
	}

	public void setExecutionsByLanguage(Map<String, Long> executionsByLanguage) {
		this.executionsByLanguage = executionsByLanguage;
	}

	public long getRunningExecutions() {
		return runningExecutions;
	}

	public void setRunningExecutions(long runningExecutions) {
		this.runningExecutions = runningExecutions;
	}

	public long getCompletedExecutions() {
		return completedExecutions;
	}

	public void setCompletedExecutions(long completedExecutions) {
		this.completedExecutions = completedExecutions;
	}

	public long getFailedExecutions() {
		return failedExecutions;
	}

	public void setFailedExecutions(long failedExecutions) {
		this.failedExecutions = failedExecutions;
	}

	public double getAverageExecutionTimeMs() {
		return averageExecutionTimeMs;
	}

	public void setAverageExecutionTimeMs(double averageExecutionTimeMs) {
		this.averageExecutionTimeMs = averageExecutionTimeMs;
	}
}
