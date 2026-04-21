package com.codesync.version.resource;

import com.codesync.version.client.ProjectPermissionClient;
import com.codesync.version.dto.CreateBranchRequest;
import com.codesync.version.dto.CreateSnapshotRequest;
import com.codesync.version.dto.DiffResponse;
import com.codesync.version.dto.ProjectPermissionDTO;
import com.codesync.version.dto.RestoreSnapshotRequest;
import com.codesync.version.dto.TagSnapshotRequest;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.exception.InvalidVersionRequestException;
import com.codesync.version.security.AuthenticatedUser;
import com.codesync.version.service.VersionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/versions")
public class VersionResource {

	private final VersionService service;
	private final ProjectPermissionClient projectPermissionClient;

	public VersionResource(VersionService service, ProjectPermissionClient projectPermissionClient) {
		this.service = service;
		this.projectPermissionClient = projectPermissionClient;
	}

	@PostMapping
	public ResponseEntity<Snapshot> createSnapshot(@Valid @RequestBody CreateSnapshotRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyWriteAccess(request.getProjectId(), authorizationHeader);
		Snapshot snapshot = service.createSnapshot(request, requireCurrentUserId(authentication));
		return ResponseEntity.status(HttpStatus.CREATED).body(snapshot);
	}

	@GetMapping("/{snapshotId}")
	public Snapshot getSnapshotById(@PathVariable Long snapshotId,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Snapshot snapshot = service.getSnapshotById(snapshotId);
		verifyReadAccess(snapshot.getProjectId(), authorizationHeader);
		return snapshot;
	}

	@GetMapping("/file/{fileId}")
	public List<Snapshot> getSnapshotsByFile(@PathVariable Long fileId,
			@RequestParam(required = false) Long projectId,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		List<Snapshot> snapshots = service.getSnapshotsByFile(fileId);
		verifyListReadAccess(snapshots, projectId, authorizationHeader);
		return snapshots;
	}

	@GetMapping("/project/{projectId}")
	public List<Snapshot> getSnapshotsByProject(@PathVariable Long projectId,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyReadAccess(projectId, authorizationHeader);
		return service.getSnapshotsByProject(projectId);
	}

	@GetMapping("/project/{projectId}/branch/{branch}")
	public List<Snapshot> getSnapshotsByBranch(@PathVariable Long projectId, @PathVariable String branch,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyReadAccess(projectId, authorizationHeader);
		return service.getSnapshotsByBranch(projectId, branch);
	}

	@GetMapping("/file/{fileId}/latest")
	public Snapshot getLatestSnapshot(@PathVariable Long fileId,
			@RequestParam(required = false) String branch,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Snapshot snapshot = service.getLatestSnapshot(fileId, branch);
		verifyReadAccess(snapshot.getProjectId(), authorizationHeader);
		return snapshot;
	}

	@GetMapping("/file/{fileId}/history")
	public List<Snapshot> getFileHistory(@PathVariable Long fileId,
			@RequestParam(required = false) Long projectId,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		List<Snapshot> snapshots = service.getFileHistory(fileId);
		verifyListReadAccess(snapshots, projectId, authorizationHeader);
		return snapshots;
	}

	@GetMapping("/diff")
	public DiffResponse diffSnapshots(@RequestParam Long fromSnapshotId, @RequestParam Long toSnapshotId,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Snapshot from = service.getSnapshotById(fromSnapshotId);
		Snapshot to = service.getSnapshotById(toSnapshotId);
		if (!from.getProjectId().equals(to.getProjectId())) {
			throw new InvalidVersionRequestException("Snapshots must belong to the same project");
		}
		verifyReadAccess(from.getProjectId(), authorizationHeader);
		return service.diffSnapshots(fromSnapshotId, toSnapshotId);
	}

	@PostMapping("/{snapshotId}/restore")
	public Snapshot restoreSnapshot(@PathVariable Long snapshotId,
			@RequestBody(required = false) RestoreSnapshotRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Snapshot source = service.getSnapshotById(snapshotId);
		verifyWriteAccess(source.getProjectId(), authorizationHeader);
		return service.restoreSnapshot(snapshotId, request, requireCurrentUserId(authentication), authorizationHeader);
	}

	@PostMapping("/branches")
	public ResponseEntity<Snapshot> createBranch(@Valid @RequestBody CreateBranchRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Snapshot source = service.getSnapshotById(request.getSourceSnapshotId());
		verifyWriteAccess(source.getProjectId(), authorizationHeader);
		Snapshot branchHead = service.createBranch(request, requireCurrentUserId(authentication));
		return ResponseEntity.status(HttpStatus.CREATED).body(branchHead);
	}

	@PostMapping("/{snapshotId}/tag")
	public Snapshot tagSnapshot(@PathVariable Long snapshotId, @Valid @RequestBody TagSnapshotRequest request,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Snapshot snapshot = service.getSnapshotById(snapshotId);
		verifyWriteAccess(snapshot.getProjectId(), authorizationHeader);
		return service.tagSnapshot(snapshotId, request.getTag());
	}

	private void verifyListReadAccess(List<Snapshot> snapshots, Long projectId, String authorizationHeader) {
		if (!snapshots.isEmpty()) {
			verifyReadAccess(snapshots.get(0).getProjectId(), authorizationHeader);
		} else if (projectId != null) {
			verifyReadAccess(projectId, authorizationHeader);
		}
	}

	private void verifyReadAccess(Long projectId, String authorizationHeader) {
		validateProjectId(projectId);
		ProjectPermissionDTO permissions = projectPermissionClient.getPermissions(projectId, authorizationHeader);
		if (!permissions.isCanRead()) {
			throw new AccessDeniedException("You do not have access to this project's version history");
		}
	}

	private void verifyWriteAccess(Long projectId, String authorizationHeader) {
		validateProjectId(projectId);
		ProjectPermissionDTO permissions = projectPermissionClient.getPermissions(projectId, authorizationHeader);
		if (!permissions.isCanWrite()) {
			throw new AccessDeniedException("You do not have permission to modify this project's versions");
		}
	}

	private void validateProjectId(Long projectId) {
		if (projectId == null || projectId <= 0) {
			throw new InvalidVersionRequestException("Project id must be greater than 0");
		}
	}

	private Long requireCurrentUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("Authentication is required");
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof AuthenticatedUser user) {
			return user.userId();
		}
		String name = authentication.getName();
		try {
			return Long.valueOf(name);
		} catch (NumberFormatException ex) {
			throw new AccessDeniedException("Unable to determine the authenticated user");
		}
	}
}
