package com.codesync.collab.entity;

import com.codesync.collab.enums.SessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collab_sessions")
public class CollabSession {

	@Id
	@Column(length = 36, nullable = false, updatable = false)
	private String sessionId;

	@Column(nullable = false)
	private Long projectId;

	@Column(nullable = false)
	private Long fileId;

	@Column(nullable = false)
	private Long ownerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SessionStatus status = SessionStatus.ACTIVE;

	@Column(length = 100)
	private String language;

	@Lob
	@Column(nullable = false)
	private String currentContent = "";

	@Column(nullable = false)
	private int currentRevision = 0;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime lastActivityAt;

	private LocalDateTime endedAt;

	@Column(nullable = false)
	private int maxParticipants = 10;

	@Column(nullable = false)
	private boolean passwordProtected = false;

	@Column(length = 255)
	private String sessionPasswordHash;

	@Version
	private long version;

	@PrePersist
	public void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		if (sessionId == null) {
			sessionId = UUID.randomUUID().toString();
		}
		if (createdAt == null) {
			createdAt = now;
		}
		if (lastActivityAt == null) {
			lastActivityAt = now;
		}
	}

	public boolean isActive() {
		return status == SessionStatus.ACTIVE;
	}

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

	public String getSessionPasswordHash() {
		return sessionPasswordHash;
	}

	public void setSessionPasswordHash(String sessionPasswordHash) {
		this.sessionPasswordHash = sessionPasswordHash;
	}
}
