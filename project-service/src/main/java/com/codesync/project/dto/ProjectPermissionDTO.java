package com.codesync.project.dto;

import com.codesync.project.enums.Visibility;

public class ProjectPermissionDTO {

	private Long projectId;
	private boolean canRead;
	private boolean canWrite;
	private boolean canManage;
	private boolean owner;
	private boolean member;
	private boolean admin;
	private boolean archived;
	private Visibility visibility;

	public ProjectPermissionDTO() {
	}

	public ProjectPermissionDTO(Long projectId, boolean canRead, boolean canWrite, boolean canManage,
			boolean owner, boolean member, boolean admin, boolean archived, Visibility visibility) {
		this.projectId = projectId;
		this.canRead = canRead;
		this.canWrite = canWrite;
		this.canManage = canManage;
		this.owner = owner;
		this.member = member;
		this.admin = admin;
		this.archived = archived;
		this.visibility = visibility;
	}

	public Long getProjectId() {
		return projectId;
	}

	public void setProjectId(Long projectId) {
		this.projectId = projectId;
	}

	public boolean isCanRead() {
		return canRead;
	}

	public void setCanRead(boolean canRead) {
		this.canRead = canRead;
	}

	public boolean isCanWrite() {
		return canWrite;
	}

	public void setCanWrite(boolean canWrite) {
		this.canWrite = canWrite;
	}

	public boolean isCanManage() {
		return canManage;
	}

	public void setCanManage(boolean canManage) {
		this.canManage = canManage;
	}

	public boolean isOwner() {
		return owner;
	}

	public void setOwner(boolean owner) {
		this.owner = owner;
	}

	public boolean isMember() {
		return member;
	}

	public void setMember(boolean member) {
		this.member = member;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}
}
