package com.codesync.project.dto;

import java.time.Instant;
import java.util.UUID;

public class ProjectMemberEvent {

	private UUID eventId;
	private Instant occurredAt;
	private Long projectId;
	private Long memberUserId;
	private Long actorId;
	private String projectName;

	public UUID getEventId() {
		return eventId;
	}

	public void setEventId(UUID eventId) {
		this.eventId = eventId;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Long getMemberUserId() {
		return memberUserId;
	}

	public void setMemberUserId(Long memberUserId) {
		this.memberUserId = memberUserId;
	}

	public Long getActorId() {
		return actorId;
	}

	public void setActorId(Long actorId) {
		this.actorId = actorId;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
}
