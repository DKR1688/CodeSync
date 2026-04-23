package com.codesync.execution.sandbox;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExecutionProcessManager {

	private final ConcurrentHashMap<UUID, Process> runningProcesses = new ConcurrentHashMap<>();
	private final Set<UUID> cancelledJobs = ConcurrentHashMap.newKeySet();

	public void register(UUID jobId, Process process) {
		runningProcesses.put(jobId, process);
		if (cancelledJobs.contains(jobId)) {
			process.destroyForcibly();
		}
	}

	public void requestCancel(UUID jobId) {
		cancelledJobs.add(jobId);
		Process process = runningProcesses.get(jobId);
		if (process != null) {
			process.destroyForcibly();
		}
	}

	public boolean isCancelRequested(UUID jobId) {
		return cancelledJobs.contains(jobId);
	}

	public void complete(UUID jobId) {
		runningProcesses.remove(jobId);
		cancelledJobs.remove(jobId);
	}
}
