package com.codesync.collab.service;

import com.codesync.collab.client.FileServiceClient;
import com.codesync.collab.client.ProjectPermissionClient;
import com.codesync.collab.dto.BroadcastChangeRequest;
import com.codesync.collab.dto.CodeFileDTO;
import com.codesync.collab.dto.CollabSessionDTO;
import com.codesync.collab.dto.CollabSocketEvent;
import com.codesync.collab.dto.CreateSessionRequest;
import com.codesync.collab.dto.CursorUpdateRequest;
import com.codesync.collab.dto.JoinSessionRequest;
import com.codesync.collab.dto.ParticipantDTO;
import com.codesync.collab.dto.ProjectPermissionDTO;
import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.enums.ParticipantRole;
import com.codesync.collab.enums.SessionStatus;
import com.codesync.collab.exception.ConflictException;
import com.codesync.collab.exception.InvalidCollabRequestException;
import com.codesync.collab.exception.ResourceNotFoundException;
import com.codesync.collab.repository.CollabSessionRepository;
import com.codesync.collab.repository.ParticipantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CollabServiceImpl implements CollabService {

	private static final int DEFAULT_MAX_PARTICIPANTS = 10;
	private static final List<String> PARTICIPANT_COLORS = List.of(
			"#2563EB",
			"#DC2626",
			"#059669",
			"#7C3AED",
			"#EA580C",
			"#0891B2",
			"#DB2777",
			"#4F46E5");

	private final CollabSessionRepository sessionRepository;
	private final ParticipantRepository participantRepository;
	private final ProjectPermissionClient projectPermissionClient;
	private final FileServiceClient fileServiceClient;
	private final SimpMessagingTemplate messagingTemplate;
	private final PasswordEncoder passwordEncoder;

	@Value("${collab.session.max-idle-minutes:30}")
	private long maxIdleMinutes;

	public CollabServiceImpl(CollabSessionRepository sessionRepository,
			ParticipantRepository participantRepository,
			ProjectPermissionClient projectPermissionClient,
			FileServiceClient fileServiceClient,
			SimpMessagingTemplate messagingTemplate,
			PasswordEncoder passwordEncoder) {
		this.sessionRepository = sessionRepository;
		this.participantRepository = participantRepository;
		this.projectPermissionClient = projectPermissionClient;
		this.fileServiceClient = fileServiceClient;
		this.messagingTemplate = messagingTemplate;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public CollabSessionDTO createSession(CreateSessionRequest request, Long actorUserId, String authorizationHeader) {
		validatePositiveId(actorUserId, "Actor user id");
		if (request == null) {
			throw new InvalidCollabRequestException("Session payload is required");
		}
		validatePositiveId(request.getProjectId(), "Project id");
		validatePositiveId(request.getFileId(), "File id");

		ProjectPermissionDTO permissions = requirePermissions(request.getProjectId(), authorizationHeader);
		if (!permissions.isCanWrite()) {
			throw new AccessDeniedException("You do not have permission to start a collaboration session");
		}

		CodeFileDTO file = fileServiceClient.getFileById(request.getFileId(), authorizationHeader);
		if (!request.getProjectId().equals(file.getProjectId())) {
			throw new InvalidCollabRequestException("The selected file does not belong to the requested project");
		}
		if (file.isDeleted()) {
			throw new InvalidCollabRequestException("Deleted files cannot be used in collaboration sessions");
		}
		if (file.isFolder()) {
			throw new InvalidCollabRequestException("Folders cannot be used in collaboration sessions");
		}

		sessionRepository.findFirstByFileIdAndStatusOrderByCreatedAtDesc(request.getFileId(), SessionStatus.ACTIVE)
				.ifPresent(existing -> {
					throw new ConflictException(
							"An active collaboration session already exists for file " + request.getFileId());
				});

		CollabSession session = new CollabSession();
		session.setSessionId(UUID.randomUUID().toString());
		session.setProjectId(request.getProjectId());
		session.setFileId(request.getFileId());
		session.setOwnerId(actorUserId);
		session.setStatus(SessionStatus.ACTIVE);
		session.setLanguage(file.getLanguage());
		session.setCurrentContent(file.getContent() == null ? "" : file.getContent());
		session.setCurrentRevision(0);
		session.setCreatedAt(LocalDateTime.now());
		session.setLastActivityAt(LocalDateTime.now());
		session.setMaxParticipants(resolveMaxParticipants(request.getMaxParticipants()));
		applySessionPassword(session, request.getPassword());

		sessionRepository.save(session);
		participantRepository.save(createParticipant(session, actorUserId, ParticipantRole.HOST));

		return toSessionDTO(session);
	}

	@Override
	@Transactional(readOnly = true)
	public CollabSessionDTO getSessionById(String sessionId, String authorizationHeader) {
		CollabSession session = getSessionOrThrow(sessionId);
		assertCanReadProject(session.getProjectId(), authorizationHeader);
		return toSessionDTO(session);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CollabSessionDTO> getSessionsByProject(Long projectId, String authorizationHeader) {
		validatePositiveId(projectId, "Project id");
		assertCanReadProject(projectId, authorizationHeader);
		return sessionRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
				.map(this::toSessionDTO)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CollabSessionDTO> getActiveSessions() {
		return sessionRepository.findByStatusOrderByCreatedAtDesc(SessionStatus.ACTIVE).stream()
				.map(this::toSessionDTO)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public CollabSessionDTO getActiveSessionForFile(Long fileId, String authorizationHeader) {
		validatePositiveId(fileId, "File id");
		CollabSession session = sessionRepository.findFirstByFileIdAndStatusOrderByCreatedAtDesc(fileId, SessionStatus.ACTIVE)
				.orElseThrow(() -> new ResourceNotFoundException("No active session found for file id " + fileId));
		assertCanReadProject(session.getProjectId(), authorizationHeader);
		return toSessionDTO(session);
	}

	@Override
	public ParticipantDTO joinSession(String sessionId, JoinSessionRequest request, Long actorUserId,
			String authorizationHeader) {
		validatePositiveId(actorUserId, "Actor user id");
		CollabSession session = requireActiveSession(sessionId);
		ProjectPermissionDTO permissions = requirePermissions(session.getProjectId(), authorizationHeader);
		if (!permissions.isCanRead()) {
			throw new AccessDeniedException("You do not have access to this collaboration session");
		}

		Participant existing = participantRepository.findBySessionSessionIdAndUserIdAndLeftAtIsNull(sessionId, actorUserId)
				.orElse(null);
		if (existing != null) {
			return toParticipantDTO(existing);
		}

		if (participantRepository.countBySessionSessionIdAndLeftAtIsNull(sessionId) >= session.getMaxParticipants()) {
			throw new ConflictException("The collaboration session is already full");
		}

		if (session.isPasswordProtected()) {
			String providedPassword = request != null ? request.getPassword() : null;
			if (!StringUtils.hasText(providedPassword)
					|| !passwordEncoder.matches(providedPassword, session.getSessionPasswordHash())) {
				throw new AccessDeniedException("The collaboration session password is invalid");
			}
		}

		ParticipantRole requestedRole = request != null ? request.getRole() : null;
		ParticipantRole role = resolveJoinRole(session, actorUserId, requestedRole, permissions);
		Participant participant = createParticipant(session, actorUserId, role);
		participantRepository.save(participant);
		touch(session);
		sessionRepository.save(session);

		ParticipantDTO dto = toParticipantDTO(participant);
		broadcastEvent(session.getSessionId(), "PARTICIPANT_JOINED", dto);
		return dto;
	}

	@Override
	public void leaveSession(String sessionId, Long actorUserId) {
		validatePositiveId(actorUserId, "Actor user id");
		CollabSession session = requireActiveSession(sessionId);
		Participant participant = requireActiveParticipant(sessionId, actorUserId);
		participant.setLeftAt(LocalDateTime.now());
		participantRepository.save(participant);
		touch(session);
		sessionRepository.save(session);
		broadcastEvent(sessionId, "PARTICIPANT_LEFT", toParticipantDTO(participant));
	}

	@Override
	public void endSession(String sessionId, Long actorUserId, boolean admin, String authorizationHeader) {
		validatePositiveId(actorUserId, "Actor user id");
		CollabSession session = requireActiveSession(sessionId);
		if (!admin && !session.getOwnerId().equals(actorUserId)) {
			throw new AccessDeniedException("Only the session owner or an admin can end this session");
		}
		if (StringUtils.hasText(authorizationHeader)) {
			fileServiceClient.updateContent(session.getFileId(), session.getCurrentContent(), authorizationHeader);
		}
		endSessionInternal(session, actorUserId, "SESSION_ENDED", Map.of("endedBy", actorUserId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ParticipantDTO> getParticipants(String sessionId, String authorizationHeader) {
		CollabSession session = getSessionOrThrow(sessionId);
		assertCanReadProject(session.getProjectId(), authorizationHeader);
		return participantRepository.findBySessionSessionIdOrderByJoinedAtAsc(sessionId).stream()
				.map(this::toParticipantDTO)
				.toList();
	}

	@Override
	public ParticipantDTO updateCursor(String sessionId, CursorUpdateRequest request, Long actorUserId) {
		validatePositiveId(actorUserId, "Actor user id");
		if (request == null) {
			throw new InvalidCollabRequestException("Cursor payload is required");
		}
		CollabSession session = requireActiveSession(sessionId);
		Participant participant = requireActiveParticipant(sessionId, actorUserId);
		applyCursor(participant, request);
		participantRepository.save(participant);
		touch(session);
		sessionRepository.save(session);

		ParticipantDTO dto = toParticipantDTO(participant);
		broadcastEvent(sessionId, "CURSOR_UPDATED", dto);
		return dto;
	}

	@Override
	public CollabSessionDTO broadcastChange(String sessionId, BroadcastChangeRequest request, Long actorUserId,
			String authorizationHeader) {
		validatePositiveId(actorUserId, "Actor user id");
		if (request == null) {
			throw new InvalidCollabRequestException("Change payload is required");
		}
		CollabSession session = requireActiveSession(sessionId);
		Participant participant = requireActiveParticipant(sessionId, actorUserId);
		if (participant.getRole() == ParticipantRole.VIEWER) {
			throw new AccessDeniedException("Viewer participants cannot edit the session content");
		}

		int baseRevision = request.getBaseRevision();
		if (baseRevision != session.getCurrentRevision()) {
			throw new ConflictException(
					"Session revision mismatch. Expected " + session.getCurrentRevision() + " but received " + baseRevision);
		}

		session.setCurrentContent(request.getContent());
		session.setCurrentRevision(session.getCurrentRevision() + 1);
		touch(session);
		if (request.getCursorLine() != null && request.getCursorCol() != null) {
			participant.setCursorLine(request.getCursorLine());
			participant.setCursorCol(request.getCursorCol());
			participant.setSelectionEndLine(request.getSelectionEndLine() != null
					? request.getSelectionEndLine()
					: request.getCursorLine());
			participant.setSelectionEndCol(request.getSelectionEndCol() != null
					? request.getSelectionEndCol()
					: request.getCursorCol());
			participantRepository.save(participant);
		}

		sessionRepository.save(session);
		fileServiceClient.updateContent(session.getFileId(), request.getContent(), authorizationHeader);

		CollabSessionDTO dto = toSessionDTO(session);
		broadcastEvent(sessionId, "CONTENT_SYNC", dto);
		return dto;
	}

	@Override
	public void kickParticipant(String sessionId, Long targetUserId, Long actorUserId, boolean admin) {
		validatePositiveId(targetUserId, "Target user id");
		validatePositiveId(actorUserId, "Actor user id");
		CollabSession session = requireActiveSession(sessionId);
		if (!admin && !session.getOwnerId().equals(actorUserId)) {
			throw new AccessDeniedException("Only the session owner or an admin can remove participants");
		}
		if (session.getOwnerId().equals(targetUserId)) {
			if (!admin) {
				throw new InvalidCollabRequestException("The session owner cannot be kicked from the session");
			}
			endSessionInternal(session, actorUserId, "SESSION_ENDED", Map.of("reason", "Ended by administrator"));
			return;
		}

		Participant participant = requireActiveTargetParticipant(sessionId, targetUserId);
		participant.setLeftAt(LocalDateTime.now());
		participantRepository.save(participant);
		touch(session);
		sessionRepository.save(session);
		broadcastEvent(sessionId, "PARTICIPANT_KICKED", toParticipantDTO(participant));
	}

	@Override
	@Scheduled(fixedDelayString = "${collab.session.cleanup-interval-ms:300000}")
	public void cleanUpInactiveSessions() {
		LocalDateTime threshold = LocalDateTime.now().minusMinutes(maxIdleMinutes);
		List<CollabSession> staleSessions = sessionRepository.findByStatusAndLastActivityAtBefore(SessionStatus.ACTIVE, threshold);
		for (CollabSession session : staleSessions) {
			endSessionInternal(session, session.getOwnerId(), "SESSION_ENDED", Map.of("reason", "Session timed out"));
		}
	}

	private CollabSession requireActiveSession(String sessionId) {
		CollabSession session = getSessionOrThrow(sessionId);
		if (!session.isActive()) {
			throw new InvalidCollabRequestException("The collaboration session has already ended");
		}
		return session;
	}

	private CollabSession getSessionOrThrow(String sessionId) {
		if (!StringUtils.hasText(sessionId)) {
			throw new InvalidCollabRequestException("Session id is required");
		}
		return sessionRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Session not found with id " + sessionId));
	}

	private Participant requireActiveParticipant(String sessionId, Long userId) {
		return participantRepository.findBySessionSessionIdAndUserIdAndLeftAtIsNull(sessionId, userId)
				.orElseThrow(() -> new AccessDeniedException("You are not an active participant in this session"));
	}

	private Participant requireActiveTargetParticipant(String sessionId, Long userId) {
		return participantRepository.findBySessionSessionIdAndUserIdAndLeftAtIsNull(sessionId, userId)
				.orElseThrow(() -> new AccessDeniedException("The target user is not an active participant in this session"));
	}

	private ProjectPermissionDTO requirePermissions(Long projectId, String authorizationHeader) {
		return projectPermissionClient.getPermissions(projectId, authorizationHeader);
	}

	private void assertCanReadProject(Long projectId, String authorizationHeader) {
		ProjectPermissionDTO permissions = requirePermissions(projectId, authorizationHeader);
		if (!permissions.isCanRead()) {
			throw new AccessDeniedException("You do not have access to this collaboration session");
		}
	}

	private void applySessionPassword(CollabSession session, String password) {
		if (StringUtils.hasText(password)) {
			session.setPasswordProtected(true);
			session.setSessionPasswordHash(passwordEncoder.encode(password));
			return;
		}
		session.setPasswordProtected(false);
		session.setSessionPasswordHash(null);
	}

	private int resolveMaxParticipants(Integer requestedValue) {
		return requestedValue == null ? DEFAULT_MAX_PARTICIPANTS : requestedValue;
	}

	private ParticipantRole resolveJoinRole(CollabSession session, Long actorUserId, ParticipantRole requestedRole,
			ProjectPermissionDTO permissions) {
		if (session.getOwnerId().equals(actorUserId)) {
			return ParticipantRole.HOST;
		}
		if (requestedRole == null) {
			return permissions.isCanWrite() ? ParticipantRole.EDITOR : ParticipantRole.VIEWER;
		}
		if (requestedRole == ParticipantRole.HOST) {
			throw new InvalidCollabRequestException("Only the session owner can be the host");
		}
		if (requestedRole == ParticipantRole.EDITOR && !permissions.isCanWrite()) {
			throw new AccessDeniedException("You do not have edit access for this project");
		}
		return requestedRole;
	}

	private Participant createParticipant(CollabSession session, Long userId, ParticipantRole role) {
		Participant participant = new Participant();
		participant.setSession(session);
		participant.setUserId(userId);
		participant.setRole(role);
		participant.setJoinedAt(LocalDateTime.now());
		participant.setCursorLine(1);
		participant.setCursorCol(1);
		participant.setSelectionEndLine(1);
		participant.setSelectionEndCol(1);
		participant.setColor(nextParticipantColor(session.getSessionId()));
		return participant;
	}

	private String nextParticipantColor(String sessionId) {
		long currentParticipants = participantRepository.findBySessionSessionIdOrderByJoinedAtAsc(sessionId).size();
		return PARTICIPANT_COLORS.get((int) (currentParticipants % PARTICIPANT_COLORS.size()));
	}

	private void applyCursor(Participant participant, CursorUpdateRequest request) {
		participant.setCursorLine(request.getCursorLine());
		participant.setCursorCol(request.getCursorCol());
		participant.setSelectionEndLine(request.getSelectionEndLine() != null
				? request.getSelectionEndLine()
				: request.getCursorLine());
		participant.setSelectionEndCol(request.getSelectionEndCol() != null
				? request.getSelectionEndCol()
				: request.getCursorCol());
	}

	private void touch(CollabSession session) {
		session.setLastActivityAt(LocalDateTime.now());
	}

	private void endSessionInternal(CollabSession session, Long actorUserId, String eventType, Object payload) {
		if (!session.isActive()) {
			return;
		}
		session.setStatus(SessionStatus.ENDED);
		session.setEndedAt(LocalDateTime.now());
		session.setLastActivityAt(LocalDateTime.now());

		List<Participant> activeParticipants = participantRepository.findBySessionSessionIdAndLeftAtIsNullOrderByJoinedAtAsc(
				session.getSessionId());
		LocalDateTime endedAt = session.getEndedAt();
		for (Participant participant : activeParticipants) {
			participant.setLeftAt(endedAt);
		}
		if (!activeParticipants.isEmpty()) {
			participantRepository.saveAll(activeParticipants);
		}
		sessionRepository.save(session);

		Map<String, Object> enrichedPayload = Map.of(
				"details", payload,
				"endedBy", actorUserId,
				"endedAt", endedAt,
				"sessionId", session.getSessionId());
		broadcastEvent(session.getSessionId(), eventType, enrichedPayload);
	}

	private void broadcastEvent(String sessionId, String eventType, Object payload) {
		messagingTemplate.convertAndSend("/topic/sessions/" + sessionId, new CollabSocketEvent(eventType, sessionId, payload));
	}

	private CollabSessionDTO toSessionDTO(CollabSession session) {
		CollabSessionDTO dto = new CollabSessionDTO();
		dto.setSessionId(session.getSessionId());
		dto.setProjectId(session.getProjectId());
		dto.setFileId(session.getFileId());
		dto.setOwnerId(session.getOwnerId());
		dto.setStatus(session.getStatus());
		dto.setLanguage(session.getLanguage());
		dto.setCurrentContent(session.getCurrentContent());
		dto.setCurrentRevision(session.getCurrentRevision());
		dto.setCreatedAt(session.getCreatedAt());
		dto.setLastActivityAt(session.getLastActivityAt());
		dto.setEndedAt(session.getEndedAt());
		dto.setMaxParticipants(session.getMaxParticipants());
		dto.setPasswordProtected(session.isPasswordProtected());
		dto.setParticipantCount(participantRepository.countBySessionSessionIdAndLeftAtIsNull(session.getSessionId()));
		return dto;
	}

	private ParticipantDTO toParticipantDTO(Participant participant) {
		ParticipantDTO dto = new ParticipantDTO();
		dto.setParticipantId(participant.getParticipantId());
		dto.setSessionId(participant.getSession().getSessionId());
		dto.setUserId(participant.getUserId());
		dto.setRole(participant.getRole());
		dto.setJoinedAt(participant.getJoinedAt());
		dto.setLeftAt(participant.getLeftAt());
		dto.setCursorLine(participant.getCursorLine());
		dto.setCursorCol(participant.getCursorCol());
		dto.setSelectionEndLine(participant.getSelectionEndLine());
		dto.setSelectionEndCol(participant.getSelectionEndCol());
		dto.setColor(participant.getColor());
		dto.setActive(participant.isActive());
		return dto;
	}

	private void validatePositiveId(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new InvalidCollabRequestException(fieldName + " must be greater than 0");
		}
	}
}
