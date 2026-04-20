package com.codesync.file.resource;

import com.codesync.file.dto.CreateFolderRequest;
import com.codesync.file.dto.FileContentUpdateRequest;
import com.codesync.file.dto.FileMoveRequest;
import com.codesync.file.dto.FileRenameRequest;
import com.codesync.file.dto.FileTreeNode;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.InvalidFileRequestException;
import com.codesync.file.security.AuthenticatedUser;
import com.codesync.file.service.FileService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/files")
public class FileResource {

	private final FileService service;

	public FileResource(FileService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<CodeFile> create(@RequestBody CodeFile file, Authentication authentication) {
		Long userId = requireCurrentUserId(authentication);
		file.setCreatedById(userId);
		file.setLastEditedBy(userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createFile(file));
	}

	@PostMapping("/folders")
	public ResponseEntity<CodeFile> createFolder(@Valid @RequestBody CreateFolderRequest request,
			Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(service.createFolder(request.getProjectId(), request.getPath(), requireCurrentUserId(authentication)));
	}

	@GetMapping("/{id}")
	public CodeFile getById(@PathVariable Long id) {
		return service.getFileById(id);
	}

	@GetMapping("/project/{projectId}")
	public List<CodeFile> getByProject(@PathVariable Long projectId) {
		return service.getFilesByProject(projectId);
	}

	@GetMapping("/{id}/content")
	public String getContent(@PathVariable Long id) {
		return service.getFileContent(id);
	}

	@PutMapping("/{id}/content")
	public CodeFile updateContent(@PathVariable Long id, @RequestBody FileContentUpdateRequest request,
			Authentication authentication) {
		return service.updateFileContent(id, request != null ? request.getContent() : null, requireCurrentUserId(authentication));
	}

	@PutMapping("/{id}/rename")
	public CodeFile rename(@PathVariable Long id, @Valid @RequestBody FileRenameRequest request) {
		return service.renameFile(id, request.getNewName());
	}

	@PutMapping("/{id}/move")
	public CodeFile move(@PathVariable Long id, @Valid @RequestBody FileMoveRequest request) {
		return service.moveFile(id, request.getNewPath());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.deleteFile(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/restore")
	public ResponseEntity<Void> restore(@PathVariable Long id) {
		service.restoreFile(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/project/{projectId}/tree")
	public List<FileTreeNode> getTree(@PathVariable Long projectId) {
		return service.getFileTree(projectId);
	}

	@GetMapping("/project/{projectId}/search")
	public List<CodeFile> search(@PathVariable Long projectId, @RequestParam String query) {
		return service.searchInProject(projectId, query);
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
