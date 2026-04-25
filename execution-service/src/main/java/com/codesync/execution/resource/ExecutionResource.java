package com.codesync.execution.resource;

import com.codesync.execution.client.ProjectPermissionClient;
import com.codesync.execution.dto.ExecutionResultDTO;
import com.codesync.execution.dto.ExecutionStatsDTO;
import com.codesync.execution.dto.LanguageRequest;
import com.codesync.execution.dto.ProjectPermissionDTO;
import com.codesync.execution.dto.SubmitExecutionRequest;
import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.enums.ExecutionStatus;
import com.codesync.execution.exception.InvalidExecutionRequestException;
import com.codesync.execution.security.AuthenticatedUser;
import com.codesync.execution.service.ExecutionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/executions")
public class ExecutionResource {

	private final ExecutionService executionService;
	private final ProjectPermissionClient projectPermissionClient;

	public ExecutionResource(ExecutionService executionService, ProjectPermissionClient projectPermissionClient) {
		this.executionService = executionService;
		this.projectPermissionClient = projectPermissionClient;
	}

	@PostMapping
	public ResponseEntity<ExecutionJob> submitExecution(@Valid @RequestBody SubmitExecutionRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyWriteAccess(request.getProjectId(), authorizationHeader);
		ExecutionJob job = executionService.submitExecution(request, requireCurrentUserId(authentication));
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
	}

	@GetMapping("/{jobId}")
	public ExecutionJob getJob(@PathVariable UUID jobId, Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		ExecutionJob job = executionService.getJobById(jobId);
		verifyJobReadAccess(job, authentication, authorizationHeader);
		return job;
	}

	@GetMapping("/{jobId}/result")
	public ExecutionResultDTO getExecutionResult(@PathVariable UUID jobId, Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		ExecutionJob job = executionService.getJobById(jobId);
		verifyJobReadAccess(job, authentication, authorizationHeader);
		return executionService.getExecutionResult(jobId);
	}

	@PostMapping("/{jobId}/cancel")
	public ExecutionJob cancelExecution(@PathVariable UUID jobId, Authentication authentication) {
		ExecutionJob job = executionService.getJobById(jobId);
		AuthenticatedUser user = requireCurrentUser(authentication);
		if (!user.isAdmin() && !job.getUserId().equals(user.userId())) {
			throw new AccessDeniedException("Only the job owner or an admin can cancel this execution");
		}
		return executionService.cancelExecution(jobId);
	}

	@GetMapping("/me")
	public List<ExecutionJob> getMyExecutions(Authentication authentication) {
		return executionService.getExecutionsByUser(requireCurrentUserId(authentication));
	}

	@GetMapping("/users/{userId}")
	public List<ExecutionJob> getExecutionsByUser(@PathVariable Long userId, Authentication authentication) {
		AuthenticatedUser currentUser = requireCurrentUser(authentication);
		if (!currentUser.isAdmin() && !userId.equals(currentUser.userId())) {
			throw new AccessDeniedException("You can only view your own execution history");
		}
		return executionService.getExecutionsByUser(userId);
	}

	@GetMapping("/projects/{projectId}")
	public List<ExecutionJob> getExecutionsByProject(@PathVariable Long projectId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUser(authentication);
		verifyReadAccess(projectId, authorizationHeader);
		return executionService.getExecutionsByProject(projectId);
	}

	@GetMapping("/languages")
	public List<SupportedLanguage> getSupportedLanguages(
			@RequestParam(name = "includeDisabled", defaultValue = "false") boolean includeDisabled,
			Authentication authentication) {
		if (includeDisabled) {
			AuthenticatedUser user = requireCurrentUser(authentication);
			if (!user.isAdmin()) {
				throw new AccessDeniedException("Only admins can view disabled languages");
			}
			return executionService.getAllLanguages();
		}
		return executionService.getSupportedLanguages();
	}

	@GetMapping("/languages/{language}/version")
	public String getLanguageVersion(@PathVariable String language) {
		return executionService.getLanguageVersion(language);
	}

	@GetMapping("/admin/jobs")
	@PreAuthorize("hasRole('ADMIN')")
	public List<ExecutionJob> getAllJobs(@RequestParam(required = false) ExecutionStatus status,
			@RequestParam(required = false) String language,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
		if (status != null) {
			return executionService.getExecutionsByStatus(status);
		}
		if (language != null) {
			return executionService.getExecutionsByLanguage(language);
		}
		if (from != null || to != null) {
			return executionService.getExecutionsBetween(from, to);
		}
		return executionService.getAllExecutions();
	}

	@GetMapping("/admin/stats")
	@PreAuthorize("hasRole('ADMIN')")
	public ExecutionStatsDTO getExecutionStats() {
		return executionService.getExecutionStats();
	}

	@PostMapping("/languages")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<SupportedLanguage> createLanguage(@Valid @RequestBody LanguageRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(executionService.saveLanguage(request));
	}

	@PutMapping("/languages/{language}")
	@PreAuthorize("hasRole('ADMIN')")
	public SupportedLanguage updateLanguage(@PathVariable String language, @Valid @RequestBody LanguageRequest request) {
		if (!language.equalsIgnoreCase(request.getLanguage())) {
			throw new InvalidExecutionRequestException("Path language must match payload language");
		}
		return executionService.saveLanguage(request);
	}

	@PatchMapping("/languages/{language}/enabled")
	@PreAuthorize("hasRole('ADMIN')")
	public SupportedLanguage setLanguageEnabled(@PathVariable String language, @RequestParam boolean enabled) {
		return executionService.setLanguageEnabled(language, enabled);
	}

	private void verifyJobReadAccess(ExecutionJob job, Authentication authentication, String authorizationHeader) {
		AuthenticatedUser user = requireCurrentUser(authentication);
		if (user.isAdmin() || job.getUserId().equals(user.userId())) {
			return;
		}
		verifyReadAccess(job.getProjectId(), authorizationHeader);
	}

	private void verifyReadAccess(Long projectId, String authorizationHeader) {
		validateProjectId(projectId);
		ProjectPermissionDTO permissions = projectPermissionClient.getPermissions(projectId, authorizationHeader);
		if (!permissions.isCanRead()) {
			throw new AccessDeniedException("You do not have access to this project's execution history");
		}
	}

	private void verifyWriteAccess(Long projectId, String authorizationHeader) {
		validateProjectId(projectId);
		ProjectPermissionDTO permissions = projectPermissionClient.getPermissions(projectId, authorizationHeader);
		if (!permissions.isCanWrite()) {
			throw new AccessDeniedException("You do not have permission to run code for this project");
		}
	}

	private void validateProjectId(Long projectId) {
		if (projectId == null || projectId <= 0) {
			throw new InvalidExecutionRequestException("Project id must be greater than 0");
		}
	}

	private Long requireCurrentUserId(Authentication authentication) {
		return requireCurrentUser(authentication).userId();
	}

	private AuthenticatedUser requireCurrentUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("Authentication is required");
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof AuthenticatedUser user) {
			return user;
		}
		String name = authentication.getName();
		try {
			return new AuthenticatedUser(Long.valueOf(name), null, "DEVELOPER");
		} catch (NumberFormatException ex) {
			throw new AccessDeniedException("Unable to determine the authenticated user");
		}
	}
}
