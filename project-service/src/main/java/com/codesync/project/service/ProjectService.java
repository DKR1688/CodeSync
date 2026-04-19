package com.codesync.project.service;

import com.codesync.project.dto.ProjectDTO;

import java.util.List;

public interface ProjectService {

	ProjectDTO createProject(ProjectDTO project);

	ProjectDTO getProjectById(Long projectId);

	List<ProjectDTO> getProjectsByOwner(Long ownerId);

	List<ProjectDTO> getPublicProjects();

	List<ProjectDTO> searchProjects(String name);

	List<ProjectDTO> getProjectsByMember(Long userId); // placeholder

	ProjectDTO updateProject(Long projectId, ProjectDTO project);

	void archiveProject(Long projectId);

	void deleteProject(Long projectId);

	ProjectDTO forkProject(Long projectId, Long newOwnerId);

	void starProject(Long projectId);

	List<ProjectDTO> getProjectsByLanguage(String language);
}