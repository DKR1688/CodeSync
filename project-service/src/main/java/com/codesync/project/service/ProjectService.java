package com.codesync.project.service;

import com.codesync.project.dto.ProjectDTO;

import java.util.List;
import java.util.Set;

public interface ProjectService {

	ProjectDTO createProject(ProjectDTO project);

	ProjectDTO getProjectById(Long projectId);

	List<ProjectDTO> getAllProjects();

	List<ProjectDTO> getProjectsByOwner(Long ownerId);

	List<ProjectDTO> getPublicProjects();

	List<ProjectDTO> searchProjects(String name);

	List<ProjectDTO> getProjectsByMember(Long userId);

	ProjectDTO updateProject(Long projectId, ProjectDTO project);

	void archiveProject(Long projectId);

	void deleteProject(Long projectId);

	ProjectDTO forkProject(Long projectId, Long newOwnerId);

	void rollbackFork(Long sourceProjectId, Long forkProjectId);

	void starProject(Long projectId);

	List<ProjectDTO> getProjectsByLanguage(String language);

	void addMember(Long projectId, Long userId);

	void removeMember(Long projectId, Long userId);

	Set<Long> getProjectMembers(Long projectId);
}
