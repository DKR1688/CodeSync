package com.codesync.collab.dto;

import com.codesync.collab.enums.ParticipantRole;

import java.time.LocalDateTime;

public class ParticipantDTO {

	private Long participantId;
	private String sessionId;
	private Long userId;
	private ParticipantRole role;
	private LocalDateTime joinedAt;
	private LocalDateTime leftAt;
	private int cursorLine;
	private int cursorCol;
	private int selectionEndLine;
	private int selectionEndCol;
	private String color;
	private boolean active;

	public Long getParticipantId() {
		return participantId;
	}

	public void setParticipantId(Long participantId) {
		this.participantId = participantId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
