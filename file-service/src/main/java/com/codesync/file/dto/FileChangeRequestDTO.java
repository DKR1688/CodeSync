package com.codesync.file.dto;

import com.codesync.file.entity.FileChangeRequest.Status;

import java.time.LocalDateTime;

public class FileChangeRequestDTO {

	private Long changeRequestId;
	private Long projectId;
	private Long fileId;
	private String filePath;
	private Long submittedBy;
	private Long reviewedBy;
	private String proposedContent;
	private String originalContent;
	private Status status;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime reviewedAt;

	public Long getChangeRequestId() {
		return changeRequestId;
	}

	public void setChangeRequestId(Long changeRequestId) {
		this.changeRequestId = changeRequestId;
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

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public Long getSubmittedBy() {
		return submittedBy;
	}

	public void setSubmittedBy(Long submittedBy) {
		this.submittedBy = submittedBy;
	}

	public Long getReviewedBy() {
		return reviewedBy;
	}

	public void setReviewedBy(Long reviewedBy) {
		this.reviewedBy = reviewedBy;
	}

	public String getProposedContent() {
		return proposedContent;
	}

	public void setProposedContent(String proposedContent) {
		this.proposedContent = proposedContent;
	}

	public String getOriginalContent() {
		return originalContent;
	}

	public void setOriginalContent(String originalContent) {
		this.originalContent = originalContent;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public LocalDateTime getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(LocalDateTime reviewedAt) {
		this.reviewedAt = reviewedAt;
	}
}
