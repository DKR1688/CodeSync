package com.codesync.comment.dto;

import java.time.Instant;
import java.util.UUID;

public class CommentEvent {

	private UUID eventId;
	private Instant occurredAt;
	private Long commentId;
	private Long projectId;
	private Long fileId;
	private Long authorId;
	private Long parentCommentId;
	private Long snapshotId;
	private Integer lineNumber;
	private Integer columnNumber;
	private boolean resolved;

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

	public Long getCommentId() {
		return commentId;
	}

	public void setCommentId(Long commentId) {
		this.commentId = commentId;
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

	public Long getAuthorId() {
		return authorId;
	}

	public void setAuthorId(Long authorId) {
		this.authorId = authorId;
	}

	public Long getParentCommentId() {
		return parentCommentId;
	}

	public void setParentCommentId(Long parentCommentId) {
		this.parentCommentId = parentCommentId;
	}

	public Long getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(Long snapshotId) {
		this.snapshotId = snapshotId;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}

	public Integer getColumnNumber() {
		return columnNumber;
	}

	public void setColumnNumber(Integer columnNumber) {
		this.columnNumber = columnNumber;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void setResolved(boolean resolved) {
		this.resolved = resolved;
	}
}
