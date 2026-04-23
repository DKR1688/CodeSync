package com.codesync.execution.service;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.enums.ExecutionStatus;
import com.codesync.execution.repository.ExecutionRepository;
import com.codesync.execution.repository.SupportedLanguageRepository;
import com.codesync.execution.sandbox.SandboxExecutionResult;
import com.codesync.execution.sandbox.SandboxRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionWorkerTest {

	@Mock
	private ExecutionRepository executionRepository;

	@Mock
	private SupportedLanguageRepository languageRepository;

	@Mock
	private SandboxRunner sandboxRunner;

	@Mock
	private ExecutionStreamPublisher streamPublisher;

	private ExecutionWorker worker;

	@BeforeEach
	void setUp() {
		worker = new ExecutionWorker(executionRepository, languageRepository, sandboxRunner, streamPublisher);
	}

	@Test
	void processJobRunsSandboxAndStoresCompletedResult() {
		UUID jobId = UUID.randomUUID();
		ExecutionJob job = job(jobId, ExecutionStatus.QUEUED);
		SupportedLanguage language = language();
		when(executionRepository.findByJobId(jobId)).thenReturn(Optional.of(job), Optional.of(job));
		when(languageRepository.findById("python")).thenReturn(Optional.of(language));
		when(sandboxRunner.run(eq(job), eq(language), any())).thenAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Consumer<String> stdoutConsumer = invocation.getArgument(2);
			stdoutConsumer.accept("streamed output");
			return new SandboxExecutionResult(ExecutionStatus.COMPLETED, "final output", "", 0, 25, 128);
		});

		worker.processJob(jobId);

		assertThat(job.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
		assertThat(job.getStdout()).isEqualTo("final output");
		assertThat(job.getExitCode()).isZero();
		assertThat(job.getExecutionTimeMs()).isEqualTo(25);
		assertThat(job.getMemoryUsedKb()).isEqualTo(128);
		assertThat(job.getStartedAt()).isNotNull();
		assertThat(job.getCompletedAt()).isNotNull();
		verify(streamPublisher).publishStatus(jobId, ExecutionStatus.RUNNING);
		verify(streamPublisher).publishStdout(jobId, "streamed output");
		verify(streamPublisher).publishStatus(jobId, ExecutionStatus.COMPLETED);
	}

	@Test
	void processJobPublishesCancelledStatusWithoutRunningSandbox() {
		UUID jobId = UUID.randomUUID();
		ExecutionJob job = job(jobId, ExecutionStatus.CANCELLED);
		when(executionRepository.findByJobId(jobId)).thenReturn(Optional.of(job));

		worker.processJob(jobId);

		verify(streamPublisher).publishStatus(jobId, ExecutionStatus.CANCELLED);
		verify(sandboxRunner, never()).run(any(), any(), any());
		verify(executionRepository, never()).save(any());
	}

	@Test
	void processJobMarksFailedWhenLanguageIsUnavailable() {
		UUID jobId = UUID.randomUUID();
		ExecutionJob job = job(jobId, ExecutionStatus.QUEUED);
		when(executionRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
		when(languageRepository.findById("python")).thenReturn(Optional.empty());

		worker.processJob(jobId);

		assertThat(job.getStatus()).isEqualTo(ExecutionStatus.FAILED);
		assertThat(job.getStderr()).contains("Language is not available");
		assertThat(job.getCompletedAt()).isNotNull();
		verify(streamPublisher).publishStatus(jobId, ExecutionStatus.FAILED);
		verify(sandboxRunner, never()).run(any(), any(), any());
	}

	@Test
	void processJobKeepsCancellationRequestedDuringSandboxRun() {
		UUID jobId = UUID.randomUUID();
		ExecutionJob job = job(jobId, ExecutionStatus.QUEUED);
		ExecutionJob cancelled = job(jobId, ExecutionStatus.CANCELLED);
		SupportedLanguage language = language();
		when(executionRepository.findByJobId(jobId)).thenReturn(Optional.of(job), Optional.of(cancelled));
		when(languageRepository.findById("python")).thenReturn(Optional.of(language));
		when(sandboxRunner.run(eq(job), eq(language), any())).thenReturn(
				new SandboxExecutionResult(ExecutionStatus.COMPLETED, "ignored", "", 0, 10, 64));

		worker.processJob(jobId);

		assertThat(cancelled.getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
		assertThat(cancelled.getExecutionTimeMs()).isEqualTo(10);
		assertThat(cancelled.getMemoryUsedKb()).isEqualTo(64);
		assertThat(cancelled.getCompletedAt()).isNotNull();
		verify(streamPublisher).publishStatus(jobId, ExecutionStatus.CANCELLED);
	}

	private ExecutionJob job(UUID jobId, ExecutionStatus status) {
		ExecutionJob job = new ExecutionJob();
		job.setJobId(jobId);
		job.setProjectId(1L);
		job.setUserId(2L);
		job.setLanguage("python");
		job.setSourceCode("print('hi')");
		job.setStatus(status);
		return job;
	}

	private SupportedLanguage language() {
		SupportedLanguage language = new SupportedLanguage();
		language.setLanguage("python");
		language.setDisplayName("Python");
		language.setRuntimeVersion("3.12");
		language.setDockerImage("python:3.12-alpine");
		language.setSourceFileName("main.py");
		language.setRunCommand("python3 main.py");
		language.setEnabled(true);
		language.setDefaultTimeLimitSeconds(10);
		language.setDefaultMemoryLimitMb(256);
		return language;
	}
}
