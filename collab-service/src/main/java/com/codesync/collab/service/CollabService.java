package com.codesync.collab.service;

import com.codesync.collab.dto.BroadcastChangeRequest;
import com.codesync.collab.dto.CollabSessionDTO;
import com.codesync.collab.dto.CreateSessionRequest;
import com.codesync.collab.dto.CursorUpdateRequest;
import com.codesync.collab.dto.JoinSessionRequest;
import com.codesync.collab.dto.ParticipantDTO;

import java.util.List;

public interface CollabService {

	CollabSessionDTO createSession(CreateSessionRequest request, Long actorUserId, String authorizationHeader);

	CollabSessionDTO getSessionById(String sessionId, String authorizationHeader);

	List<CollabSessionDTO> getSessionsByProject(Long projectId, String authorizationHeader);

	List<CollabSessionDTO> getActiveSessions();

	CollabSessionDTO getActiveSessionForFile(Long fileId, String authorizationHeader);

	ParticipantDTO joinSession(String sessionId, JoinSessionRequest request, Long actorUserId, String authorizationHeader);

	void leaveSession(String sessionId, Long actorUserId);

	void endSession(String sessionId, Long actorUserId, boolean admin, String authorizationHeader);

	List<ParticipantDTO> getParticipants(String sessionId, String authorizationHeader);

	ParticipantDTO updateCursor(String sessionId, CursorUpdateRequest request, Long actorUserId);

	CollabSessionDTO broadcastChange(String sessionId, BroadcastChangeRequest request, Long actorUserId,
			String authorizationHeader);

	void kickParticipant(String sessionId, Long targetUserId, Long actorUserId, boolean admin,
			String authorizationHeader);

	void cleanUpInactiveSessions();
}
