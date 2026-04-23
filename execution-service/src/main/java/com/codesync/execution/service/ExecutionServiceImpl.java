package com.codesync.execution.service;

import com.codesync.execution.dto.ExecutionResultDTO;
import com.codesync.execution.dto.ExecutionStatsDTO;
import com.codesync.execution.dto.LanguageRequest;
import com.codesync.execution.dto.SubmitExecutionRequest;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.enums.ExecutionStatus;
import com.codesync.execution.exception.InvalidExecutionRequestException;
import com.codesync.execution.exception.ResourceNotFoundException;
import com.codesync.execution.queue.ExecutionQueue;
import com.codesync.execution.repository.ExecutionRepository;
import com.codesync.execution.repository.SupportedLanguageRepository;
import com.codesync.execution.sandbox.ExecutionProcessManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ExecutionServiceImpl implements ExecutionService {

	private final ExecutionRepository executionRepository;
	private final SupportedLanguageRepository languageRepository;
	private final ExecutionQueue executionQueue;
	private final ExecutionProcessManager processManager;
	private final int defaultTimeLimitSeconds;
	private final int defaultMemoryLimitMb;
	private final double defaultCpuLimit;
	private final int maxTimeLimitSeconds;
	private final int maxMemoryLimitMb;
	private final int maxSourceBytes;
	private final int maxStdinBytes;

	public ExecutionServiceImpl(ExecutionRepository executionRepository,
			SupportedLanguageRepository languageRepository,
			ExecutionQueue executionQueue,
			ExecutionProcessManager processManager,
			@Value("${codesync.execution.default-time-limit-seconds:10}") int defaultTimeLimitSeconds,
			@Value("${codesync.execution.default-memory-limit-mb:256}") int defaultMemoryLimitMb,
			@Value("${codesync.execution.default-cpu-limit:1.0}") double defaultCpuLimit,
			@Value("${codesync.execution.max-time-limit-seconds:10}") int maxTimeLimitSeconds,
			@Value("${codesync.execution.max-memory-limit-mb:256}") int maxMemoryLimitMb,
			@Value("${codesync.execution.max-source-bytes:1048576}") int maxSourceBytes,
			@Value("${codesync.execution.max-stdin-bytes:65536}") int maxStdinBytes) {
		this.executionRepository = executionRepository;
		this.languageRepository = languageRepository;
		this.executionQueue = executionQueue;
		this.processManager = processManager;
		this.defaultTimeLimitSeconds = defaultTimeLimitSeconds;
		this.defaultMemoryLimitMb = defaultMemoryLimitMb;
		this.defaultCpuLimit = defaultCpuLimit;
		this.maxTimeLimitSeconds = maxTimeLimitSeconds;
		this.maxMemoryLimitMb = maxMemoryLimitMb;
		this.maxSourceBytes = maxSourceBytes;
		this.maxStdinBytes = maxStdinBytes;
	}

	@Override
	public ExecutionJob submitExecution(SubmitExecutionRequest request, Long userId) {
		if (request == null) {
			throw new InvalidExecutionRequestException("Execution payload is required");
		}
		validatePositiveId(request.getProjectId(), "Project id");
		validatePositiveId(userId, "User id");
		if (request.getFileId() != null) {
			validatePositiveId(request.getFileId(), "File id");
		}

		String languageKey = normalizeLanguage(request.getLanguage());
		SupportedLanguage language = getEnabledLanguage(languageKey);
		String sourceCode = request.getSourceCode() == null ? "" : request.getSourceCode();
		String stdin = request.getStdin() == null ? "" : request.getStdin();
		validatePayloadSize(sourceCode, maxSourceBytes, "Source code");
		validatePayloadSize(stdin, maxStdinBytes, "stdin");

		ExecutionJob job = new ExecutionJob();
		job.setProjectId(request.getProjectId());
		job.setFileId(request.getFileId());
		job.setUserId(userId);
		job.setLanguage(language.getLanguage());
		job.setSourceCode(sourceCode);
		job.setStdin(stdin);
		job.setStatus(ExecutionStatus.QUEUED);
		job.setTimeLimitSeconds(resolveTimeLimit(request.getTimeLimitSeconds(), language));
		job.setMemoryLimitMb(resolveMemoryLimit(request.getMemoryLimitMb(), language));
		job.setCpuLimit(resolveCpuLimit(request.getCpuLimit()));

		ExecutionJob saved = executionRepository.save(job);
		enqueueAfterCommit(saved.getJobId());
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public ExecutionJob getJobById(UUID jobId) {
		if (jobId == null) {
			throw new InvalidExecutionRequestException("Job id is required");
		}
		return executionRepository.findByJobId(jobId)
				.orElseThrow(() -> new ResourceNotFoundException("Execution job not found with id " + jobId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExecutionJob> getExecutionsByUser(Long userId) {
		validatePositiveId(userId, "User id");
		return executionRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExecutionJob> getExecutionsByProject(Long projectId) {
		validatePositiveId(projectId, "Project id");
		return executionRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExecutionJob> getAllExecutions() {
		return executionRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExecutionJob> getExecutionsByStatus(ExecutionStatus status) {
		if (status == null) {
			throw new InvalidExecutionRequestException("Status is required");
		}
		return executionRepository.findByStatusOrderByCreatedAtAsc(status);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExecutionJob> getExecutionsByLanguage(String language) {
		return executionRepository.findByLanguageOrderByCreatedAtDesc(normalizeLanguage(language));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExecutionJob> getExecutionsBetween(LocalDateTime from, LocalDateTime to) {
		if (from == null || to == null || from.isAfter(to)) {
			throw new InvalidExecutionRequestException("Valid from and to timestamps are required");
		}
		return executionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
	}

	@Override
	public ExecutionJob cancelExecution(UUID jobId) {
		ExecutionJob job = getJobById(jobId);
		if (isTerminal(job.getStatus())) {
			return job;
		}
		job.setStatus(ExecutionStatus.CANCELLED);
		job.setCompletedAt(LocalDateTime.now());
		job.setStderr(appendLine(job.getStderr(), "Execution was cancelled."));
		processManager.requestCancel(job.getJobId());
		return executionRepository.save(job);
	}

	@Override
	@Transactional(readOnly = true)
	public ExecutionResultDTO getExecutionResult(UUID jobId) {
		ExecutionJob job = getJobById(jobId);
		return new ExecutionResultDTO(job.getJobId(), job.getStatus(), job.getStdout(), job.getStderr(),
				job.getExitCode(), job.getExecutionTimeMs(), job.getMemoryUsedKb(), job.getCompletedAt());
	}

	@Override
	@Transactional(readOnly = true)
	public List<SupportedLanguage> getSupportedLanguages() {
		return languageRepository.findByEnabledTrueOrderByDisplayNameAsc();
	}

	@Override
	@Transactional(readOnly = true)
	public List<SupportedLanguage> getAllLanguages() {
		return languageRepository.findAllByOrderByDisplayNameAsc();
	}

	@Override
	@Transactional(readOnly = true)
	public String getLanguageVersion(String language) {
		return getLanguage(normalizeLanguage(language)).getRuntimeVersion();
	}

	@Override
	public SupportedLanguage saveLanguage(LanguageRequest request) {
		if (request == null) {
			throw new InvalidExecutionRequestException("Language payload is required");
		}
		SupportedLanguage language = new SupportedLanguage();
		language.setLanguage(normalizeLanguage(request.getLanguage()));
		language.setDisplayName(normalizeRequired(request.getDisplayName(), "Display name", 100));
		language.setRuntimeVersion(normalizeRequired(request.getRuntimeVersion(), "Runtime version", 100));
		language.setDockerImage(normalizeRequired(request.getDockerImage(), "Docker image", 200));
		language.setSourceFileName(normalizeRequired(request.getSourceFileName(), "Source file name", 100));
		language.setRunCommand(normalizeRequired(request.getRunCommand(), "Run command", 1000));
		language.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
		language.setDefaultTimeLimitSeconds(resolveAdminTimeLimit(request.getDefaultTimeLimitSeconds()));
		language.setDefaultMemoryLimitMb(resolveAdminMemoryLimit(request.getDefaultMemoryLimitMb()));
		return languageRepository.save(language);
	}

	@Override
	public SupportedLanguage setLanguageEnabled(String language, boolean enabled) {
		SupportedLanguage supportedLanguage = getLanguage(normalizeLanguage(language));
		supportedLanguage.setEnabled(enabled);
		return languageRepository.save(supportedLanguage);
	}

	@Override
	@Transactional(readOnly = true)
	public ExecutionStatsDTO getExecutionStats() {
		List<ExecutionJob> jobs = executionRepository.findAll();
		ExecutionStatsDTO stats = new ExecutionStatsDTO();
		stats.setTotalExecutions(jobs.size());

		for (ExecutionStatus status : ExecutionStatus.values()) {
			stats.getExecutionsByStatus().put(status, 0L);
		}

		Map<String, Long> byLanguage = new LinkedHashMap<>();
		long timedJobs = 0;
		long totalTime = 0;
		for (ExecutionJob job : jobs) {
			stats.getExecutionsByStatus().merge(job.getStatus(), 1L, Long::sum);
			byLanguage.merge(job.getLanguage(), 1L, Long::sum);
			if (job.getExecutionTimeMs() != null) {
				timedJobs++;
				totalTime += job.getExecutionTimeMs();
			}
		}
		stats.setExecutionsByLanguage(byLanguage);
		stats.setRunningExecutions(stats.getExecutionsByStatus().getOrDefault(ExecutionStatus.RUNNING, 0L));
		stats.setCompletedExecutions(stats.getExecutionsByStatus().getOrDefault(ExecutionStatus.COMPLETED, 0L));
		stats.setFailedExecutions(stats.getExecutionsByStatus().getOrDefault(ExecutionStatus.FAILED, 0L)
				+ stats.getExecutionsByStatus().getOrDefault(ExecutionStatus.TIMED_OUT, 0L));
		stats.setAverageExecutionTimeMs(timedJobs == 0 ? 0 : (double) totalTime / timedJobs);
		return stats;
	}

	private SupportedLanguage getEnabledLanguage(String language) {
		SupportedLanguage supportedLanguage = getLanguage(language);
		if (!supportedLanguage.isEnabled()) {
			throw new InvalidExecutionRequestException("Language is disabled: " + supportedLanguage.getDisplayName());
		}
		return supportedLanguage;
	}

	private SupportedLanguage getLanguage(String language) {
		return languageRepository.findById(language)
				.orElseThrow(() -> new ResourceNotFoundException("Unsupported language: " + language));
	}

	private String normalizeLanguage(String language) {
		if (!StringUtils.hasText(language)) {
			throw new InvalidExecutionRequestException("Language is required");
		}
		String normalized = language.trim().toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "py", "python3" -> "python";
			case "js", "node", "nodejs" -> "javascript";
			case "ts" -> "typescript";
			case "c++", "cplusplus" -> "cpp";
			default -> normalized;
		};
	}

	private void validatePayloadSize(String value, int maxBytes, String fieldName) {
		int size = value.getBytes(StandardCharsets.UTF_8).length;
		if (size > maxBytes) {
			throw new InvalidExecutionRequestException(fieldName + " must be " + maxBytes + " bytes or fewer");
		}
	}

	private int resolveTimeLimit(Integer requested, SupportedLanguage language) {
		int candidate = requested != null ? requested
				: (language.getDefaultTimeLimitSeconds() != null ? language.getDefaultTimeLimitSeconds()
						: defaultTimeLimitSeconds);
		if (candidate < 1 || candidate > maxTimeLimitSeconds) {
			throw new InvalidExecutionRequestException("Time limit must be between 1 and "
					+ maxTimeLimitSeconds + " seconds");
		}
		return candidate;
	}

	private int resolveMemoryLimit(Integer requested, SupportedLanguage language) {
		int candidate = requested != null ? requested
				: (language.getDefaultMemoryLimitMb() != null ? language.getDefaultMemoryLimitMb()
						: defaultMemoryLimitMb);
		if (candidate < 16 || candidate > maxMemoryLimitMb) {
			throw new InvalidExecutionRequestException("Memory limit must be between 16 and "
					+ maxMemoryLimitMb + " MB");
		}
		return candidate;
	}

	private double resolveCpuLimit(Double requested) {
		double candidate = requested != null ? requested : defaultCpuLimit;
		if (candidate < 0.1 || candidate > 4.0) {
			throw new InvalidExecutionRequestException("CPU limit must be between 0.1 and 4.0");
		}
		return candidate;
	}

	private int resolveAdminTimeLimit(Integer requested) {
		int candidate = requested != null ? requested : defaultTimeLimitSeconds;
		if (candidate < 1 || candidate > maxTimeLimitSeconds) {
			throw new InvalidExecutionRequestException("Default time limit must be between 1 and "
					+ maxTimeLimitSeconds + " seconds");
		}
		return candidate;
	}

	private int resolveAdminMemoryLimit(Integer requested) {
		int candidate = requested != null ? requested : defaultMemoryLimitMb;
		if (candidate < 16 || candidate > maxMemoryLimitMb) {
			throw new InvalidExecutionRequestException("Default memory limit must be between 16 and "
					+ maxMemoryLimitMb + " MB");
		}
		return candidate;
	}

	private String normalizeRequired(String value, String fieldName, int maxLength) {
		if (!StringUtils.hasText(value)) {
			throw new InvalidExecutionRequestException(fieldName + " is required");
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new InvalidExecutionRequestException(fieldName + " must be " + maxLength + " characters or fewer");
		}
		return normalized;
	}

	private void validatePositiveId(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new InvalidExecutionRequestException(fieldName + " must be greater than 0");
		}
	}

	private boolean isTerminal(ExecutionStatus status) {
		return Arrays.asList(ExecutionStatus.COMPLETED, ExecutionStatus.FAILED,
				ExecutionStatus.TIMED_OUT, ExecutionStatus.CANCELLED).contains(status);
	}

	private void enqueueAfterCommit(UUID jobId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			executionQueue.enqueue(jobId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				executionQueue.enqueue(jobId);
			}
		});
	}

	private String appendLine(String value, String line) {
		String base = value == null ? "" : value;
		if (base.contains(line)) {
			return base;
		}
		return base + line + System.lineSeparator();
	}
}
