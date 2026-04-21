package com.codesync.version.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "snapshots", indexes = {
		@Index(name = "idx_snapshot_project", columnList = "projectId"),
		@Index(name = "idx_snapshot_file", columnList = "fileId"),
		@Index(name = "idx_snapshot_author", columnList = "authorId"),
		@Index(name = "idx_snapshot_branch", columnList = "branch"),
		@Index(name = "idx_snapshot_hash", columnList = "hash"),
		@Index(name = "idx_snapshot_tag", columnList = "tag")
})
public class Snapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long snapshotId;

	@Column(nullable = false)
	private Long projectId;

	@Column(nullable = false)
	private Long fileId;

	@Column(nullable = false)
	private Long authorId;

	@Column(nullable = false, length = 1000)
	private String message;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content = "";

	@Column(nullable = false, length = 64)
	private String hash;

	private Long parentSnapshotId;

	@Column(nullable = false, length = 100)
	private String branch = "main";

	@Column(length = 100)
	private String tag;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public Snapshot() {
	}

	@PrePersist
	public void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}

	public Long getSnapshotId() {
		return snapshotId;
	}

	public void setSnapshotId(Long snapshotId) {
		this.snapshotId = snapshotId;
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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public Long getParentSnapshotId() {
		return parentSnapshotId;
	}

	public void setParentSnapshotId(Long parentSnapshotId) {
		this.parentSnapshotId = parentSnapshotId;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Snapshot other)) {
			return false;
		}
		return Objects.equals(snapshotId, other.snapshotId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(snapshotId);
	}
}
