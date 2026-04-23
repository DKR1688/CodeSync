package com.codesync.file.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "code_files")
public class CodeFile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long fileId;

	@Column(nullable = false)
	private Long projectId;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false, length = 1024)
	private String path;

	@Column(length = 100)
	private String language;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content = "";

	@Column(nullable = false)
	private long size;

	@Column(nullable = false, updatable = false)
	private Long createdById;

	@Column(nullable = false)
	private Long lastEditedBy;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Column(nullable = false)
	private boolean isDeleted = false;

	public CodeFile() {
	}

	@PrePersist
	public void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	@Transient
	public boolean isFolder() {
		return language == null;
	}

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long fileId) {
		this.fileId = fileId;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public Long getCreatedById() {
		return createdById;
	}

	public void setCreatedById(Long createdById) {
		this.createdById = createdById;
	}

	public Long getLastEditedBy() {
		return lastEditedBy;
	}

	public void setLastEditedBy(Long lastEditedBy) {
		this.lastEditedBy = lastEditedBy;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean deleted) {
		isDeleted = deleted;
	}

	@Override
	public String toString() {
		return "CodeFile{" +
				"fileId=" + fileId +
				", projectId=" + projectId +
				", path='" + path + '\'' +
				", language='" + language + '\'' +
				", isDeleted=" + isDeleted +
				'}';
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CodeFile other)) {
			return false;
		}
		return Objects.equals(fileId, other.fileId);
	}
}
