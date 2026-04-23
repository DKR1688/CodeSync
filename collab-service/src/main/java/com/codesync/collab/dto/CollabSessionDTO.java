package com.codesync.collab.dto;

import com.codesync.collab.enums.SessionStatus;

import java.time.LocalDateTime;

public class CollabSessionDTO {

	private String sessionId;
	private Long projectId;
	private Long fileId;
	private Long ownerId;
	private SessionStatus status;
	private String language;
	private String currentContent;
	private int currentRevision;
	private LocalDateTime createdAt;
	private LocalDateTime lastActivityAt;
	private LocalDateTime endedAt;
	private int maxParticipants;
	private boolean passwordProtected;
	private long participantCount;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long fileId) {
		this.fileId = fileId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public SessionStatus getStatus() {
		return status;
	}

	public void setStatus(SessionStatus status) {
		this.status = status;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCurrentContent() {
		return currentContent;
	}

	public void setCurrentContent(String currentContent) {
		this.currentContent = currentContent;
	}

	public int getCurrentRevision() {
		return currentRevision;
	}

	public void setCurrentRevision(int currentRevision) {
		this.currentRevision = currentRevision;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getLastActivityAt() {
		return lastActivityAt;
	}

	public void setLastActivityAt(LocalDateTime lastActivityAt) {
		this.lastActivityAt = lastActivityAt;
	}

	public LocalDateTime getEndedAt() {
		return endedAt;
	}

	public void setEndedAt(LocalDateTime endedAt) {
		this.endedAt = endedAt;
	}

	public int getMaxParticipants() {
		return maxParticipants;
	}

	public void setMaxParticipants(int maxParticipants) {
		this.maxParticipants = maxParticipants;
	}

	public boolean isPasswordProtected() {
		return passwordProtected;
	}

	public void setPasswordProtected(boolean passwordProtected) {
		this.passwordProtected = passwordProtected;
	}

	public long getParticipantCount() {
		return participantCount;
	}

	public void setParticipantCount(long participantCount) {
		this.participantCount = participantCount;
	}
}
