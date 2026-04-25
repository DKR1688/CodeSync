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
import com.codesync.collab.repository.CollabSessionRepository;
import com.codesync.collab.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollabServiceImplTest {

	@Mock
	private CollabSessionRepository sessionRepository;

	@Mock
	private ParticipantRepository participantRepository;

	@Mock
	private ProjectPermissionClient projectPermissionClient;

	@Mock
	private FileServiceClient fileServiceClient;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private CollabServiceImpl service;

	private ProjectPermissionDTO writablePermissions;
	private CodeFileDTO codeFile;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "maxIdleMinutes", 30L);

		writablePermissions = new ProjectPermissionDTO();
		writablePermissions.setProjectId(10L);
		writablePermissions.setCanRead(true);
		writablePermissions.setCanWrite(true);

		codeFile = new CodeFileDTO();
		codeFile.setFileId(20L);
		codeFile.setProjectId(10L);
		codeFile.setLanguage("Java");
		codeFile.setContent("class App {}");
	}

	@Test
	void createSessionSeedsFileContentAndHostParticipant() {
		CreateSessionRequest request = new CreateSessionRequest();
		request.setProjectId(10L);
		request.setFileId(20L);
		request.setMaxParticipants(5);
		request.setPassword("secret");

		when(projectPermissionClient.getPermissions(10L, "Bearer token")).thenReturn(writablePermissions);
		when(fileServiceClient.getFileById(20L, "Bearer token")).thenReturn(codeFile);
		when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
		when(sessionRepository.save(any(CollabSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
			Participant participant = invocation.getArgument(0);
			participant.setParticipantId(1L);
			return participant;
		});
		when(participantRepository.countBySessionSessionIdAndLeftAtIsNull(anyString())).thenReturn(1L);
		when(participantRepository.findBySessionSessionIdOrderByJoinedAtAsc(anyString())).thenReturn(List.of());

		CollabSessionDTO created = service.createSession(request, 99L, "Bearer token");

		assertNotNull(created.getSessionId());
		assertEquals(10L, created.getProjectId());
		assertEquals(20L, created.getFileId());
		assertEquals("class App {}", created.getCurrentContent());
		assertEquals(1L, created.getParticipantCount());
		assertTrue(created.isPasswordProtected());
	}

	@Test
	void joinSessionAddsEditorParticipantAndBroadcastsPresence() {
		CollabSession session = activeSession();
		JoinSessionRequest request = new JoinSessionRequest();
		request.setRole(ParticipantRole.EDITOR);

		when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
		when(projectPermissionClient.getPermissions(10L, "Bearer token")).thenReturn(writablePermissions);
		when(participantRepository.findBySessionSessionIdAndUserIdAndLeftAtIsNull(session.getSessionId(), 55L))
				.thenReturn(Optional.empty());
		when(participantRepository.countBySessionSessionIdAndLeftAtIsNull(session.getSessionId())).thenReturn(1L);
		when(participantRepository.findBySessionSessionIdOrderByJoinedAtAsc(session.getSessionId())).thenReturn(List.of());
		when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
			Participant participant = invocation.getArgument(0);
			participant.setParticipantId(2L);
			return participant;
		});
		when(sessionRepository.save(any(CollabSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ParticipantDTO participant = service.joinSession(session.getSessionId(), request, 55L, "Bearer token");

		assertEquals(55L, participant.getUserId());
		assertEquals(ParticipantRole.EDITOR, participant.getRole());
		verify(messagingTemplate).convertAndSend(anyString(), any(CollabSocketEvent.class));
	}

	@Test
	void joinSessionReportsProjectPermissionMismatchClearly() {
		CollabSession session = activeSession();
		ProjectPermissionDTO deniedPermissions = new ProjectPermissionDTO();
		deniedPermissions.setProjectId(10L);
		deniedPermissions.setCanRead(false);

		when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
		when(projectPermissionClient.getPermissions(10L, "Bearer other-user")).thenReturn(deniedPermissions);

		AccessDeniedException ex = assertThrows(AccessDeniedException.class,
				() -> service.joinSession(session.getSessionId(), new JoinSessionRequest(), 55L, "Bearer other-user"));

		assertEquals("User 55 does not have access to project 10. Add this user to the project or use a token for the project owner/member before joining the collaboration session.",
				ex.getMessage());
		verify(participantRepository, never()).save(any(Participant.class));
	}

	@Test
	void updateCursorBeforeJoinReportsSameTokenJoinRequirement() {
		CollabSession session = activeSession();
		CursorUpdateRequest request = new CursorUpdateRequest();
		request.setCursorLine(1);
		request.setCursorCol(2);

		when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
		when(participantRepository.findBySessionSessionIdAndUserIdAndLeftAtIsNull(session.getSessionId(), 55L))
				.thenReturn(Optional.empty());

		AccessDeniedException ex = assertThrows(AccessDeniedException.class,
				() -> service.updateCursor(session.getSessionId(), request, 55L));

		assertEquals("User 55 is not an active participant in this session. Join the session with the same token before updating cursor or content.",
				ex.getMessage());
	}

	@Test
	void broadcastChangeUpdatesRevisionPersistsFileAndBroadcastsSync() {
		CollabSession session = activeSession();
		Participant participant = activeParticipant(session, 77L, ParticipantRole.EDITOR);
		BroadcastChangeRequest request = new BroadcastChangeRequest();
		request.setBaseRevision(0);
		request.setContent("System.out.println(\"hi\");");
		request.setCursorLine(3);
		request.setCursorCol(5);

		when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
		when(participantRepository.findBySessionSessionIdAndUserIdAndLeftAtIsNull(session.getSessionId(), 77L))
				.thenReturn(Optional.of(participant));
		when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(sessionRepository.save(any(CollabSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(participantRepository.countBySessionSessionIdAndLeftAtIsNull(session.getSessionId())).thenReturn(1L);

		CollabSessionDTO updated = service.broadcastChange(session.getSessionId(), request, 77L, "Bearer token");

		assertEquals(1, updated.getCurrentRevision());
		assertEquals("System.out.println(\"hi\");", updated.getCurrentContent());
		assertEquals(3, participant.getCursorLine());
		assertEquals(5, participant.getCursorCol());
		verify(fileServiceClient).updateContent(20L, "System.out.println(\"hi\");", "Bearer token");
		verify(messagingTemplate).convertAndSend(anyString(), any(CollabSocketEvent.class));
	}

	@Test
	void broadcastChangeRejectsStaleRevision() {
		CollabSession session = activeSession();
		session.setCurrentRevision(4);
		Participant participant = activeParticipant(session, 77L, ParticipantRole.EDITOR);
		BroadcastChangeRequest request = new BroadcastChangeRequest();
		request.setBaseRevision(1);
		request.setContent("new");

		when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
		when(participantRepository.findBySessionSessionIdAndUserIdAndLeftAtIsNull(session.getSessionId(), 77L))
				.thenReturn(Optional.of(participant));

		assertThrows(ConflictException.class,
				() -> service.broadcastChange(session.getSessionId(), request, 77L, "Bearer token"));
		verify(fileServiceClient, never()).updateContent(anyLong(), anyString(), anyString());
	}

	@Test
	void cleanupEndsTimedOutSessionsAndMarksParticipantsLeft() {
		CollabSession staleSession = activeSession();
		staleSession.setLastActivityAt(LocalDateTime.now().minusMinutes(31));
		Participant participant = activeParticipant(staleSession, 10L, ParticipantRole.HOST);

		when(sessionRepository.findByStatusAndLastActivityAtBefore(any(), any())).thenReturn(List.of(staleSession));
		when(participantRepository.findBySessionSessionIdAndLeftAtIsNullOrderByJoinedAtAsc(staleSession.getSessionId()))
				.thenReturn(List.of(participant));
		when(sessionRepository.save(any(CollabSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.cleanUpInactiveSessions();

		assertEquals(SessionStatus.ENDED, staleSession.getStatus());
		assertNotNull(staleSession.getEndedAt());
		assertNotNull(participant.getLeftAt());
		verify(participantRepository).saveAll(any());
		verify(messagingTemplate).convertAndSend(anyString(), any(CollabSocketEvent.class));
	}

	@Test
	void ownerLeavingSessionDoesNotEndIt() {
		CollabSession session = activeSession();
		Participant host = activeParticipant(session, 10L, ParticipantRole.HOST);

		when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
		when(participantRepository.findBySessionSessionIdAndUserIdAndLeftAtIsNull(session.getSessionId(), 10L))
				.thenReturn(Optional.of(host));
		when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(sessionRepository.save(any(CollabSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.leaveSession(session.getSessionId(), 10L);

		assertEquals(SessionStatus.ACTIVE, session.getStatus());
		assertNotNull(host.getLeftAt());
		assertFalse(host.isActive());
		verify(messagingTemplate).convertAndSend(anyString(), any(CollabSocketEvent.class));
	}

	@Test
	void kickParticipantReportsInactiveTargetClearly() {
		CollabSession session = activeSession();

		when(sessionRepository.findById(session.getSessionId())).thenReturn(Optional.of(session));
		when(projectPermissionClient.getPermissions(10L, "Bearer token")).thenReturn(writablePermissions);
		when(participantRepository.findBySessionSessionIdAndUserIdAndLeftAtIsNull(session.getSessionId(), 88L))
				.thenReturn(Optional.empty());

		AccessDeniedException ex = assertThrows(AccessDeniedException.class,
				() -> service.kickParticipant(session.getSessionId(), 88L, 10L, false, "Bearer token"));

		assertEquals("The target user is not an active participant in this session", ex.getMessage());
	}

	private CollabSession activeSession() {
		CollabSession session = new CollabSession();
		session.setSessionId("session-1");
		session.setProjectId(10L);
		session.setFileId(20L);
		session.setOwnerId(10L);
		session.setStatus(SessionStatus.ACTIVE);
		session.setLanguage("Java");
		session.setCurrentContent("class App {}");
		session.setCurrentRevision(0);
		session.setCreatedAt(LocalDateTime.now().minusMinutes(2));
		session.setLastActivityAt(LocalDateTime.now().minusMinutes(1));
		session.setMaxParticipants(5);
		return session;
	}

	private Participant activeParticipant(CollabSession session, Long userId, ParticipantRole role) {
		Participant participant = new Participant();
		participant.setParticipantId(1L);
		participant.setSession(session);
		participant.setUserId(userId);
		participant.setRole(role);
		participant.setJoinedAt(LocalDateTime.now().minusMinutes(1));
		participant.setColor("#2563EB");
		return participant;
	}
}
