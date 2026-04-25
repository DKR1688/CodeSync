package com.codesync.comment.resource;

import com.codesync.comment.client.ProjectPermissionClient;
import com.codesync.comment.dto.AddCommentRequest;
import com.codesync.comment.dto.ProjectPermissionDTO;
import com.codesync.comment.dto.UpdateCommentRequest;
import com.codesync.comment.entity.Comment;
import com.codesync.comment.exception.InvalidCommentRequestException;
import com.codesync.comment.security.AuthenticatedUser;
import com.codesync.comment.service.CommentService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/comments")
public class CommentResource {

	private final CommentService service;
	private final ProjectPermissionClient projectPermissionClient;

	public CommentResource(CommentService service, ProjectPermissionClient projectPermissionClient) {
		this.service = service;
		this.projectPermissionClient = projectPermissionClient;
	}

	@PostMapping
	public ResponseEntity<Comment> add(@Valid @RequestBody AddCommentRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		verifyWriteAccess(request.getProjectId(), authorizationHeader);
		Comment comment = toComment(request, requireCurrentUserId(authentication));
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(service.addComment(comment, authorizationHeader));
	}

	@GetMapping("/{id}")
	public Comment getById(@PathVariable Long id,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		Comment comment = service.getCommentById(id);
		verifyReadAccess(comment.getProjectId(), authorizationHeader);
		return comment;
	}

	@GetMapping("/file/{fileId}")
	public List<Comment> getByFile(@PathVariable Long fileId,
			@RequestParam(required = false) Long projectId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		List<Comment> comments = service.getByFile(fileId);
		verifyListReadAccess(comments, projectId, authorizationHeader);
		return comments;
	}

	@GetMapping("/project/{projectId}")
	public List<Comment> getByProject(@PathVariable Long projectId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		verifyReadAccess(projectId, authorizationHeader);
		return service.getByProject(projectId);
	}

	@GetMapping("/{id}/replies")
	public List<Comment> getReplies(@PathVariable Long id,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		Comment comment = service.getCommentById(id);
		verifyReadAccess(comment.getProjectId(), authorizationHeader);
		return service.getReplies(id);
	}

	@GetMapping("/file/{fileId}/line/{lineNumber}")
	public List<Comment> getByLine(@PathVariable Long fileId, @PathVariable Integer lineNumber,
			@RequestParam(required = false) Long projectId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		List<Comment> comments = service.getByLine(fileId, lineNumber);
		verifyListReadAccess(comments, projectId, authorizationHeader);
		return comments;
	}

	@GetMapping("/file/{fileId}/count")
	public long getCommentCount(@PathVariable Long fileId,
			@RequestParam(required = false) Long projectId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		if (projectId != null) {
			verifyReadAccess(projectId, authorizationHeader);
		}
		return service.getCommentCount(fileId);
	}

	@GetMapping("/resolved")
	public List<Comment> getByResolved(@RequestParam(required = false) Long projectId,
			@RequestParam(defaultValue = "false") boolean resolved,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		if (projectId != null) {
			verifyReadAccess(projectId, authorizationHeader);
		}
		return service.getByResolved(projectId, resolved);
	}

	@PutMapping("/{id}")
	public Comment update(@PathVariable Long id, @Valid @RequestBody UpdateCommentRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Comment comment = service.getCommentById(id);
		ProjectPermissionDTO permissions = verifyWriteAccess(comment.getProjectId(), authorizationHeader);
		assertCanModifyComment(comment, authentication, permissions);
		return service.updateComment(id, request.getContent());
	}

	@PutMapping("/{id}/resolve")
	public Comment resolve(@PathVariable Long id,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Comment comment = service.getCommentById(id);
		verifyWriteAccess(comment.getProjectId(), authorizationHeader);
		return service.resolveComment(id);
	}

	@PutMapping("/{id}/unresolve")
	public Comment unresolve(@PathVariable Long id,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Comment comment = service.getCommentById(id);
		verifyWriteAccess(comment.getProjectId(), authorizationHeader);
		return service.unresolveComment(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Comment comment = service.getCommentById(id);
		ProjectPermissionDTO permissions = verifyWriteAccess(comment.getProjectId(), authorizationHeader);
		assertCanModifyComment(comment, authentication, permissions);
		service.deleteComment(id);
		return ResponseEntity.noContent().build();
	}

	private Comment toComment(AddCommentRequest request, Long authorId) {
		Comment comment = new Comment();
		comment.setProjectId(request.getProjectId());
		comment.setFileId(request.getFileId());
		comment.setAuthorId(authorId);
		comment.setContent(request.getContent());
		comment.setLineNumber(request.getLineNumber());
		comment.setColumnNumber(request.getColumnNumber());
		comment.setParentCommentId(request.getParentCommentId());
		comment.setSnapshotId(request.getSnapshotId());
		return comment;
	}

	private void verifyListReadAccess(List<Comment> comments, Long projectId, String authorizationHeader) {
		if (!comments.isEmpty()) {
			verifyReadAccess(comments.getFirst().getProjectId(), authorizationHeader);
		} else if (projectId != null) {
			verifyReadAccess(projectId, authorizationHeader);
		}
	}

	private ProjectPermissionDTO verifyReadAccess(Long projectId, String authorizationHeader) {
		validateProjectId(projectId);
		ProjectPermissionDTO permissions = projectPermissionClient.getPermissions(projectId, authorizationHeader);
		if (!permissions.isCanRead()) {
			throw new AccessDeniedException("You do not have access to this project's comments");
		}
		return permissions;
	}

	private ProjectPermissionDTO verifyWriteAccess(Long projectId, String authorizationHeader) {
		validateProjectId(projectId);
		ProjectPermissionDTO permissions = projectPermissionClient.getPermissions(projectId, authorizationHeader);
		if (!permissions.isCanWrite()) {
			throw new AccessDeniedException("You do not have permission to modify this project's comments");
		}
		return permissions;
	}

	private void assertCanModifyComment(Comment comment, Authentication authentication, ProjectPermissionDTO permissions) {
		Long currentUserId = requireCurrentUserId(authentication);
		if (!comment.getAuthorId().equals(currentUserId) && !permissions.isCanManage() && !isAdmin(authentication)) {
			throw new AccessDeniedException("You can only edit or delete your own comments");
		}
	}

	private void validateProjectId(Long projectId) {
		if (projectId == null || projectId <= 0) {
			throw new InvalidCommentRequestException("Project id must be greater than 0");
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

	private boolean isAdmin(Authentication authentication) {
		Object principal = authentication != null ? authentication.getPrincipal() : null;
		if (principal instanceof AuthenticatedUser user) {
			return user.isAdmin();
		}
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
	}
}
