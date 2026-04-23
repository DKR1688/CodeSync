package com.codesync.execution.service;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.enums.ExecutionStatus;
import com.codesync.execution.repository.ExecutionRepository;
import com.codesync.execution.repository.SupportedLanguageRepository;
import com.codesync.execution.sandbox.SandboxExecutionResult;
import com.codesync.execution.sandbox.SandboxRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExecutionWorker {

	private final ExecutionRepository executionRepository;
	private final SupportedLanguageRepository languageRepository;
	private final SandboxRunner sandboxRunner;
	private final ExecutionStreamPublisher streamPublisher;

	public ExecutionWorker(ExecutionRepository executionRepository,
			SupportedLanguageRepository languageRepository,
			SandboxRunner sandboxRunner,
			ExecutionStreamPublisher streamPublisher) {
		this.executionRepository = executionRepository;
		this.languageRepository = languageRepository;
		this.sandboxRunner = sandboxRunner;
		this.streamPublisher = streamPublisher;
	}

	public void processJob(UUID jobId) {
		Optional<ExecutionJob> optionalJob = executionRepository.findByJobId(jobId);
		if (optionalJob.isEmpty()) {
			return;
		}
		ExecutionJob job = optionalJob.get();
		if (job.getStatus() == ExecutionStatus.CANCELLED) {
			streamPublisher.publishStatus(jobId, ExecutionStatus.CANCELLED);
			return;
		}

		Optional<SupportedLanguage> optionalLanguage = languageRepository.findById(job.getLanguage());
		if (optionalLanguage.isEmpty() || !optionalLanguage.get().isEnabled()) {
			markFailed(job, "Language is not available: " + job.getLanguage());
			return;
		}

		job.setStatus(ExecutionStatus.RUNNING);
		job.setStartedAt(LocalDateTime.now());
		executionRepository.save(job);
		streamPublisher.publishStatus(jobId, ExecutionStatus.RUNNING);

		SandboxExecutionResult result = sandboxRunner.run(job, optionalLanguage.get(),
				chunk -> streamPublisher.publishStdout(jobId, chunk));
		ExecutionJob latest = executionRepository.findByJobId(jobId).orElse(job);
		if (latest.getStatus() == ExecutionStatus.CANCELLED) {
			latest.setExecutionTimeMs(result.getExecutionTimeMs());
			latest.setMemoryUsedKb(result.getMemoryUsedKb());
			latest.setCompletedAt(LocalDateTime.now());
			executionRepository.save(latest);
			streamPublisher.publishStatus(jobId, ExecutionStatus.CANCELLED);
			return;
		}

		latest.setStatus(result.getStatus());
		latest.setStdout(result.getStdout());
		latest.setStderr(result.getStderr());
		latest.setExitCode(result.getExitCode());
		latest.setExecutionTimeMs(result.getExecutionTimeMs());
		latest.setMemoryUsedKb(result.getMemoryUsedKb());
		latest.setCompletedAt(LocalDateTime.now());
		executionRepository.save(latest);
		streamPublisher.publishStatus(jobId, result.getStatus());
	}

	private void markFailed(ExecutionJob job, String message) {
		job.setStatus(ExecutionStatus.FAILED);
		job.setStderr(message);
		job.setCompletedAt(LocalDateTime.now());
		executionRepository.save(job);
		streamPublisher.publishStatus(job.getJobId(), ExecutionStatus.FAILED);
	}
}
