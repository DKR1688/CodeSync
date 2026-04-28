package com.codesync.collab.resource;

import com.codesync.collab.dto.BroadcastChangeRequest;
import com.codesync.collab.dto.CollabSessionDTO;
import com.codesync.collab.dto.CreateSessionRequest;
import com.codesync.collab.dto.CursorUpdateRequest;
import com.codesync.collab.dto.JoinSessionRequest;
import com.codesync.collab.dto.ParticipantDTO;
import com.codesync.collab.security.AuthenticatedUser;
import com.codesync.collab.service.CollabService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/sessions")
public class CollabResource {

	private final CollabService service;

	public CollabResource(CollabService service) {
		this.service = service;
	}

	@PostMapping
	@Operation(summary = "Create session", tags = { "01. Create Session" })
	public ResponseEntity<CollabSessionDTO> createSession(@Valid @RequestBody CreateSessionRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(service.createSession(request, requireCurrentUserId(authentication), authorizationHeader));
	}

	@GetMapping("/active")
	@Operation(summary = "Active sessions", tags = { "12. List Active Sessions" })
	public List<CollabSessionDTO> getActiveSessions(Authentication authentication) {
		assertAdmin(authentication);
		return service.getActiveSessions();
	}

	@GetMapping("/{sessionId}")
	@Operation(summary = "Session by id", tags = { "02. Get Session By Id" })
	public CollabSessionDTO getSessionById(@PathVariable String sessionId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		return service.getSessionById(sessionId, authorizationHeader);
	}

	@GetMapping("/project/{projectId}")
	@Operation(summary = "Sessions by project", tags = { "03. Get Sessions By Project" })
	public List<CollabSessionDTO> getSessionsByProject(@PathVariable Long projectId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		return service.getSessionsByProject(projectId, authorizationHeader);
	}

	@GetMapping("/file/{fileId}/active")
	@Operation(summary = "Active session by file", tags = { "04. Get Active Session For File" })
	public ResponseEntity<CollabSessionDTO> getActiveSessionForFile(@PathVariable Long fileId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		CollabSessionDTO session = service.getActiveSessionForFile(fileId, authorizationHeader);
		return session != null ? ResponseEntity.ok(session) : ResponseEntity.noContent().build();
	}

	@PostMapping("/{sessionId}/join")
	@Operation(summary = "Join session", tags = { "05. Join Session" })
	public ParticipantDTO joinSession(@PathVariable String sessionId,
			@RequestBody(required = false) JoinSessionRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		return service.joinSession(sessionId, request, requireCurrentUserId(authentication), authorizationHeader);
	}

	@PostMapping("/{sessionId}/leave")
	@Operation(summary = "Leave session", tags = { "09. Leave Session" })
	public ResponseEntity<Void> leaveSession(@PathVariable String sessionId, Authentication authentication) {
		service.leaveSession(sessionId, requireCurrentUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{sessionId}/end")
	@Operation(summary = "End session", tags = { "11. End Session" })
	public ResponseEntity<Void> endSession(@PathVariable String sessionId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		service.endSession(sessionId, requireCurrentUserId(authentication), isAdmin(authentication), authorizationHeader);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{sessionId}/participants")
	@Operation(summary = "Participants", tags = { "06. Get Participants" })
	public List<ParticipantDTO> getParticipants(@PathVariable String sessionId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		requireCurrentUserId(authentication);
		return service.getParticipants(sessionId, authorizationHeader);
	}

	@PutMapping("/{sessionId}/cursor")
	@Operation(summary = "Update cursor", tags = { "07. Update Cursor" })
	public ParticipantDTO updateCursor(@PathVariable String sessionId,
			@Valid @RequestBody CursorUpdateRequest request,
			Authentication authentication) {
		return service.updateCursor(sessionId, request, requireCurrentUserId(authentication));
	}

	@PutMapping("/{sessionId}/content")
	@Operation(summary = "Broadcast change", tags = { "08. Broadcast Content Change" })
	public CollabSessionDTO broadcastChange(@PathVariable String sessionId,
			@Valid @RequestBody BroadcastChangeRequest request,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		return service.broadcastChange(sessionId, request, requireCurrentUserId(authentication), authorizationHeader);
	}

	@PostMapping("/{sessionId}/participants/{userId}/kick")
	@Operation(summary = "Kick participant", tags = { "10. Kick Participant" })
	public ResponseEntity<Void> kickParticipant(@PathVariable String sessionId, @PathVariable Long userId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		service.kickParticipant(sessionId, userId, requireCurrentUserId(authentication), isAdmin(authentication),
				authorizationHeader);
		return ResponseEntity.noContent().build();
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

	private void assertAdmin(Authentication authentication) {
		if (!isAdmin(authentication)) {
			throw new AccessDeniedException("Administrator access is required");
		}
	}
}
