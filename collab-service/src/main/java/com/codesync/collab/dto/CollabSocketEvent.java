package com.codesync.collab.dto;

import java.time.LocalDateTime;

public class CollabSocketEvent {

	private String type;
	private String sessionId;
	private LocalDateTime occurredAt;
	private Object payload;

	public CollabSocketEvent() {
	}

	public CollabSocketEvent(String type, String sessionId, Object payload) {
		this.type = type;
		this.sessionId = sessionId;
		this.payload = payload;
		this.occurredAt = LocalDateTime.now();
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(LocalDateTime occurredAt) {
		this.occurredAt = occurredAt;
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload) {
		this.payload = payload;
	}
}
