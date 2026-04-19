package com.codesync.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.codesync.project.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class ProjectDTO {

	private Long projectId;
	private Long ownerId;

	@NotBlank(message = "Project name is required")
	@Size(max = 150, message = "Project name must be at most 150 characters")
	private String name;

	@Size(max = 1000, message = "Description must be at most 1000 characters")
	private String description;

	@Size(max = 100, message = "Language must be at most 100 characters")
	private String language;

	@NotNull(message = "Visibility is required")
	private Visibility visibility;
	private Long templateId;

	@JsonProperty("isArchived")
	private boolean isArchived;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private int starCount;
	private int forkCount;
	private Set<Long> memberUserIds = new LinkedHashSet<>();

	public ProjectDTO() {
	}

	// Getters & Setters
	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}

	public Long getTemplateId() {
		return templateId;
	}

	public void setTemplateId(Long templateId) {
		this.templateId = templateId;
	}

	public boolean isArchived() {
		return isArchived;
	}

	public void setArchived(boolean archived) {
		isArchived = archived;
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

	public int getStarCount() {
		return starCount;
	}

	public void setStarCount(int starCount) {
		this.starCount = starCount;
	}

	public int getForkCount() {
		return forkCount;
	}

	public void setForkCount(int forkCount) {
		this.forkCount = forkCount;
	}

	public Set<Long> getMemberUserIds() {
		return memberUserIds;
	}

	public void setMemberUserIds(Set<Long> memberUserIds) {
		this.memberUserIds = memberUserIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(memberUserIds);
	}
}
