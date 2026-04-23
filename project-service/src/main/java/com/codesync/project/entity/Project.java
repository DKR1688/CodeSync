package com.codesync.project.entity;

import com.codesync.project.enums.Visibility;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long projectId;

	@Column(nullable = false)
	private Long ownerId;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(length = 1000)
	private String description;

	@Column(length = 100)
	private String language;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Visibility visibility;

	private Long templateId;

	@Column(nullable = false)
	private boolean isArchived = false;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Column(nullable = false)
	private int starCount = 0;

	@Column(nullable = false)
	private int forkCount = 0;

	@ElementCollection
	@CollectionTable(name = "project_members", joinColumns = @JoinColumn(name = "project_id"))
	@Column(name = "member_user_id", nullable = false)
	private Set<Long> memberUserIds = new LinkedHashSet<>();

	// Constructors
	public Project() {
	}

	public Project(Long projectId, Long ownerId, String name, String description, String language,
			Visibility visibility, Long templateId, boolean isArchived, LocalDateTime createdAt,
			LocalDateTime updatedAt, int starCount, int forkCount) {
		this.projectId = projectId;
		this.ownerId = ownerId;
		this.name = name;
		this.description = description;
		this.language = language;
		this.visibility = visibility;
		this.templateId = templateId;
		this.isArchived = isArchived;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.starCount = starCount;
		this.forkCount = forkCount;
	}

	// Lifecycle hooks
	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	public void onUpdate() {
		this.updatedAt = LocalDateTime.now();
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

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
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
