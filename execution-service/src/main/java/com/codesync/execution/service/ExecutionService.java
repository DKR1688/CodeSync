package com.codesync.execution.service;

import com.codesync.execution.dto.ExecutionResultDTO;
import com.codesync.execution.dto.ExecutionStatsDTO;
import com.codesync.execution.dto.LanguageRequest;
import com.codesync.execution.dto.SubmitExecutionRequest;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.enums.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ExecutionService {

	ExecutionJob submitExecution(SubmitExecutionRequest request, Long userId);

	ExecutionJob getJobById(UUID jobId);

	List<ExecutionJob> getExecutionsByUser(Long userId);

	List<ExecutionJob> getExecutionsByProject(Long projectId);

	List<ExecutionJob> getAllExecutions();

	List<ExecutionJob> getExecutionsByStatus(ExecutionStatus status);

	List<ExecutionJob> getExecutionsByLanguage(String language);

	List<ExecutionJob> getExecutionsBetween(LocalDateTime from, LocalDateTime to);

	ExecutionJob cancelExecution(UUID jobId);

	ExecutionResultDTO getExecutionResult(UUID jobId);

	List<SupportedLanguage> getSupportedLanguages();

	List<SupportedLanguage> getAllLanguages();

	String getLanguageVersion(String language);

	SupportedLanguage saveLanguage(LanguageRequest request);

	SupportedLanguage setLanguageEnabled(String language, boolean enabled);

	ExecutionStatsDTO getExecutionStats();
}
