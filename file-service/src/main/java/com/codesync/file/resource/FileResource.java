package com.codesync.file.resource;

import com.codesync.file.client.ProjectPermissionClient;
import com.codesync.file.dto.CreateFolderRequest;
import com.codesync.file.dto.CopyProjectFilesRequest;
import com.codesync.file.dto.FileContentUpdateRequest;
import com.codesync.file.dto.FileMoveRequest;
import com.codesync.file.dto.FileRenameRequest;
import com.codesync.file.dto.FileTreeNode;
import com.codesync.file.dto.ProjectPermissionDTO;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.InvalidFileRequestException;
import com.codesync.file.security.AuthenticatedUser;
import com.codesync.file.service.FileService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/files")
public class FileResource {

	private final FileService service;
	private final ProjectPermissionClient projectPermissionClient;

	public FileResource(FileService service, ProjectPermissionClient projectPermissionClient) {
		this.service = service;
		this.projectPermissionClient = projectPermissionClient;
	}

	@PostMapping
	public ResponseEntity<CodeFile> create(@RequestBody CodeFile file, Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyWriteAccess(file.getProjectId(), authorizationHeader);
		Long userId = requireCurrentUserId(authentication);
		file.setCreatedById(userId);
		file.setLastEditedBy(userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createFile(file));
	}

	@PostMapping("/folders")
	public ResponseEntity<CodeFile> createFolder(@Valid @RequestBody CreateFolderRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyWriteAccess(request.getProjectId(), authorizationHeader);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(service.createFolder(request.getProjectId(), request.getPath(), requireCurrentUserId(authentication)));
	}

	@PostMapping("/projects/copy")
	public ResponseEntity<Void> copyProjectFiles(@Valid @RequestBody CopyProjectFilesRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyReadAccess(request.getSourceProjectId(), authorizationHeader);
		verifyWriteAccess(request.getTargetProjectId(), authorizationHeader);
		service.copyProjectFiles(request.getSourceProjectId(), request.getTargetProjectId(),
				requireCurrentUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	public CodeFile getById(@PathVariable Long id,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		CodeFile file = service.getFileById(id);
		verifyReadAccess(file.getProjectId(), authorizationHeader);
		return file;
	}

	@GetMapping("/project/{projectId}")
	public List<CodeFile> getByProject(@PathVariable Long projectId,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyReadAccess(projectId, authorizationHeader);
		return service.getFilesByProject(projectId);
	}

	@GetMapping("/{id}/content")
	public String getContent(@PathVariable Long id,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		CodeFile file = service.getFileById(id);
		verifyReadAccess(file.getProjectId(), authorizationHeader);
		return file.getContent();
	}

	@PutMapping("/{id}/content")
	public CodeFile updateContent(@PathVariable Long id, @RequestBody FileContentUpdateRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		CodeFile file = service.getFileById(id);
		verifyWriteAccess(file.getProjectId(), authorizationHeader);
		return service.updateFileContent(id, request != null ? request.getContent() : null, requireCurrentUserId(authentication));
	}

	@PutMapping("/{id}/rename")
	public CodeFile rename(@PathVariable Long id, @Valid @RequestBody FileRenameRequest request,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		CodeFile file = service.getFileById(id);
		verifyWriteAccess(file.getProjectId(), authorizationHeader);
		return service.renameFile(id, request.getNewName());
	}

	@PutMapping("/{id}/move")
	public CodeFile move(@PathVariable Long id, @Valid @RequestBody FileMoveRequest request,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		CodeFile file = service.getFileById(id);
		verifyWriteAccess(file.getProjectId(), authorizationHeader);
		return service.moveFile(id, request.getNewPath());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		CodeFile file = service.getFileById(id);
		verifyWriteAccess(file.getProjectId(), authorizationHeader);
		service.deleteFile(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/restore")
	public ResponseEntity<Void> restore(@PathVariable Long id,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		CodeFile file = service.getFileById(id);
		verifyWriteAccess(file.getProjectId(), authorizationHeader);
		service.restoreFile(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/project/{projectId}/tree")
	public List<FileTreeNode> getTree(@PathVariable Long projectId,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyReadAccess(projectId, authorizationHeader);
		return service.getFileTree(projectId);
	}

	@GetMapping("/project/{projectId}/search")
	public List<CodeFile> search(@PathVariable Long projectId, @RequestParam String query,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyReadAccess(projectId, authorizationHeader);
		return service.searchInProject(projectId, query);
	}

	private void verifyReadAccess(Long projectId, String authorizationHeader) {
		validateProjectId(projectId);
		ProjectPermissionDTO permissions = projectPermissionClient.getPermissions(projectId, authorizationHeader);
		if (!permissions.isCanRead()) {
			throw new AccessDeniedException("You do not have access to this project's files");
		}
	}

	private void verifyWriteAccess(Long projectId, String authorizationHeader) {
		validateProjectId(projectId);
		ProjectPermissionDTO permissions = projectPermissionClient.getPermissions(projectId, authorizationHeader);
		if (!permissions.isCanWrite()) {
			throw new AccessDeniedException("You do not have permission to modify this project's files");
		}
	}

	private void validateProjectId(Long projectId) {
		if (projectId == null || projectId <= 0) {
			throw new InvalidFileRequestException("Project id must be greater than 0");
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
			throw new InvalidFileRequestException("Unable to determine the authenticated user");
		}
	}
}
