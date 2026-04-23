package com.codesync.collab.entity;

import com.codesync.collab.enums.ParticipantRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "collab_participants")
public class Participant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long participantId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	private CollabSession session;

	@Column(nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ParticipantRole role;

	@Column(nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	private LocalDateTime leftAt;

	@Column(nullable = false)
	private int cursorLine = 1;

	@Column(nullable = false)
	private int cursorCol = 1;

	@Column(nullable = false)
	private int selectionEndLine = 1;

	@Column(nullable = false)
	private int selectionEndCol = 1;

	@Column(nullable = false, length = 20)
	private String color;

	@PrePersist
	public void onCreate() {
		if (joinedAt == null) {
			joinedAt = LocalDateTime.now();
		}
	}

	public boolean isActive() {
		return leftAt == null;
	}

	public Long getParticipantId() {
		return participantId;
	}

	public void setParticipantId(Long participantId) {
		this.participantId = participantId;
	}

	public CollabSession getSession() {
		return session;
	}

	public void setSession(CollabSession session) {
		this.session = session;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public ParticipantRole getRole() {
		return role;
	}

	public void setRole(ParticipantRole role) {
		this.role = role;
	}

	public LocalDateTime getJoinedAt() {
		return joinedAt;
	}

	public void setJoinedAt(LocalDateTime joinedAt) {
		this.joinedAt = joinedAt;
	}

	public LocalDateTime getLeftAt() {
		return leftAt;
	}

	public void setLeftAt(LocalDateTime leftAt) {
		this.leftAt = leftAt;
	}

	public int getCursorLine() {
		return cursorLine;
	}

	public void setCursorLine(int cursorLine) {
		this.cursorLine = cursorLine;
	}

	public int getCursorCol() {
		return cursorCol;
	}

	public void setCursorCol(int cursorCol) {
		this.cursorCol = cursorCol;
	}

	public int getSelectionEndLine() {
		return selectionEndLine;
	}

	public void setSelectionEndLine(int selectionEndLine) {
		this.selectionEndLine = selectionEndLine;
	}

	public int getSelectionEndCol() {
		return selectionEndCol;
	}

	public void setSelectionEndCol(int selectionEndCol) {
		this.selectionEndCol = selectionEndCol;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
}
