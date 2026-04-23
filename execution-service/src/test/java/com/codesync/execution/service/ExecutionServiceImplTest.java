package com.codesync.execution.service;

import com.codesync.execution.dto.ExecutionStatsDTO;
import com.codesync.execution.dto.LanguageRequest;
import com.codesync.execution.dto.SubmitExecutionRequest;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.enums.ExecutionStatus;
import com.codesync.execution.exception.InvalidExecutionRequestException;
import com.codesync.execution.queue.ExecutionQueue;
import com.codesync.execution.repository.ExecutionRepository;
import com.codesync.execution.repository.SupportedLanguageRepository;
import com.codesync.execution.sandbox.ExecutionProcessManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionServiceImplTest {

	@Mock
	private ExecutionRepository executionRepository;

	@Mock
	private SupportedLanguageRepository languageRepository;

	@Mock
	private ExecutionQueue executionQueue;

	@Mock
	private ExecutionProcessManager processManager;

	private ExecutionServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new ExecutionServiceImpl(executionRepository, languageRepository, executionQueue, processManager,
				10, 256, 1.0, 10, 256, 100, 50);
	}

	@Test
	void submitExecutionNormalizesLanguageAppliesDefaultsAndQueuesJob() {
		SupportedLanguage python = language("python", true, 7, 128);
		when(languageRepository.findById("python")).thenReturn(Optional.of(python));
		when(executionRepository.save(any(ExecutionJob.class))).thenAnswer(invocation -> {
			ExecutionJob job = invocation.getArgument(0);
			job.setJobId(UUID.randomUUID());
			return job;
		});

		SubmitExecutionRequest request = new SubmitExecutionRequest();
		request.setProjectId(11L);
		request.setFileId(22L);
		request.setLanguage("PY");
		request.setSourceCode("print('hi')");
		request.setStdin("input");

		ExecutionJob saved = service.submitExecution(request, 33L);

		assertThat(saved.getProjectId()).isEqualTo(11L);
		assertThat(saved.getFileId()).isEqualTo(22L);
		assertThat(saved.getUserId()).isEqualTo(33L);
		assertThat(saved.getLanguage()).isEqualTo("python");
		assertThat(saved.getStatus()).isEqualTo(ExecutionStatus.QUEUED);
		assertThat(saved.getTimeLimitSeconds()).isEqualTo(7);
		assertThat(saved.getMemoryLimitMb()).isEqualTo(128);
		assertThat(saved.getCpuLimit()).isEqualTo(1.0);
		verify(executionQueue).enqueue(saved.getJobId());
	}

	@Test
	void submitExecutionRejectsDisabledLanguageWithoutSaving() {
		when(languageRepository.findById("python")).thenReturn(Optional.of(language("python", false, 10, 256)));

		SubmitExecutionRequest request = new SubmitExecutionRequest();
		request.setProjectId(1L);
		request.setLanguage("python");
		request.setSourceCode("print('hi')");

		assertThatThrownBy(() -> service.submitExecution(request, 2L))
				.isInstanceOf(InvalidExecutionRequestException.class)
				.hasMessageContaining("disabled");
		verify(executionRepository, never()).save(any());
		verify(executionQueue, never()).enqueue(any());
	}

	@Test
	void submitExecutionRejectsPayloadsOverConfiguredSize() {
		when(languageRepository.findById("python")).thenReturn(Optional.of(language("python", true, 10, 256)));

		SubmitExecutionRequest request = new SubmitExecutionRequest();
		request.setProjectId(1L);
		request.setLanguage("python");
		request.setSourceCode("x".repeat(101));

		assertThatThrownBy(() -> service.submitExecution(request, 2L))
				.isInstanceOf(InvalidExecutionRequestException.class)
				.hasMessageContaining("Source code");
		verify(executionRepository, never()).save(any());
	}

	@Test
	void cancelExecutionMarksRunningJobAndRequestsProcessCancellation() {
		UUID jobId = UUID.randomUUID();
		ExecutionJob job = job(jobId, 1L, "python", ExecutionStatus.RUNNING, null);
		when(executionRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
		when(executionRepository.save(any(ExecutionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ExecutionJob cancelled = service.cancelExecution(jobId);

		assertThat(cancelled.getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
		assertThat(cancelled.getCompletedAt()).isNotNull();
		assertThat(cancelled.getStderr()).contains("cancelled");
		verify(processManager).requestCancel(jobId);
		verify(executionRepository).save(job);
	}

	@Test
	void cancelExecutionLeavesTerminalJobUnchanged() {
		UUID jobId = UUID.randomUUID();
		ExecutionJob job = job(jobId, 1L, "python", ExecutionStatus.COMPLETED, 42L);
		when(executionRepository.findByJobId(jobId)).thenReturn(Optional.of(job));

		ExecutionJob result = service.cancelExecution(jobId);

		assertThat(result.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
		verify(processManager, never()).requestCancel(any());
		verify(executionRepository, never()).save(any());
	}

	@Test
	void saveLanguageNormalizesAndPersistsRegistryEntry() {
		when(languageRepository.save(any(SupportedLanguage.class))).thenAnswer(invocation -> invocation.getArgument(0));
		LanguageRequest request = new LanguageRequest();
		request.setLanguage("NodeJS");
		request.setDisplayName("Node.js");
		request.setRuntimeVersion("22");
		request.setDockerImage("node:22-alpine");
		request.setSourceFileName("main.js");
		request.setRunCommand("node main.js");
		request.setEnabled(true);

		SupportedLanguage saved = service.saveLanguage(request);

		ArgumentCaptor<SupportedLanguage> captor = ArgumentCaptor.forClass(SupportedLanguage.class);
		verify(languageRepository).save(captor.capture());
		assertThat(saved.getLanguage()).isEqualTo("javascript");
		assertThat(captor.getValue().getDefaultTimeLimitSeconds()).isEqualTo(10);
		assertThat(captor.getValue().getDefaultMemoryLimitMb()).isEqualTo(256);
	}

	@Test
	void getExecutionStatsAggregatesByStatusAndLanguage() {
		when(executionRepository.findAll()).thenReturn(List.of(
				job(UUID.randomUUID(), 1L, "python", ExecutionStatus.COMPLETED, 100L),
				job(UUID.randomUUID(), 2L, "python", ExecutionStatus.FAILED, 50L),
				job(UUID.randomUUID(), 3L, "java", ExecutionStatus.RUNNING, null),
				job(UUID.randomUUID(), 4L, "java", ExecutionStatus.TIMED_OUT, 150L)));

		ExecutionStatsDTO stats = service.getExecutionStats();

		assertThat(stats.getTotalExecutions()).isEqualTo(4);
		assertThat(stats.getCompletedExecutions()).isEqualTo(1);
		assertThat(stats.getRunningExecutions()).isEqualTo(1);
		assertThat(stats.getFailedExecutions()).isEqualTo(2);
		assertThat(stats.getExecutionsByLanguage()).containsEntry("python", 2L).containsEntry("java", 2L);
		assertThat(stats.getAverageExecutionTimeMs()).isEqualTo(100.0);
	}

	private SupportedLanguage language(String key, boolean enabled, int timeLimit, int memoryLimit) {
		SupportedLanguage language = new SupportedLanguage();
		language.setLanguage(key);
		language.setDisplayName(key);
		language.setRuntimeVersion("test");
		language.setDockerImage(key + ":test");
		language.setSourceFileName("main.txt");
		language.setRunCommand("run");
		language.setEnabled(enabled);
		language.setDefaultTimeLimitSeconds(timeLimit);
		language.setDefaultMemoryLimitMb(memoryLimit);
		return language;
	}

	private ExecutionJob job(UUID jobId, Long userId, String language, ExecutionStatus status, Long executionTimeMs) {
		ExecutionJob job = new ExecutionJob();
		job.setJobId(jobId);
		job.setProjectId(1L);
		job.setUserId(userId);
		job.setLanguage(language);
		job.setStatus(status);
		job.setExecutionTimeMs(executionTimeMs);
		return job;
	}
}
