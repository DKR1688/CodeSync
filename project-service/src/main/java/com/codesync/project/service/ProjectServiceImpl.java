package com.codesync.project.service;

import com.codesync.project.dto.ProjectDTO;
import com.codesync.project.entity.Project;
import com.codesync.project.enums.Visibility;
import com.codesync.project.exception.InvalidProjectRequestException;
import com.codesync.project.exception.ResourceNotFoundException;
import com.codesync.project.repository.ProjectRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectServiceImpl implements ProjectService {

	private static final Sort DISCOVERY_SORT = Sort.by(
			Sort.Order.desc("starCount"),
			Sort.Order.desc("forkCount"),
			Sort.Order.desc("updatedAt"));

	private static final Sort RECENT_FIRST_SORT = Sort.by(Sort.Order.desc("updatedAt"));

	private final ProjectRepository repository;

	public ProjectServiceImpl(ProjectRepository repository) {
		this.repository = repository;
	}

	@Override
	public ProjectDTO createProject(ProjectDTO dto) {
		validateProject(dto, true);

		Project project = new Project();
		applyMutableFields(project, dto, true);
		project.setArchived(false);
		project.setStarCount(0);
		project.setForkCount(0);

		return toDTO(repository.save(project));
	}

	@Override
	@Transactional(readOnly = true)
	public ProjectDTO getProjectById(Long id) {
		validatePositiveId(id, "Project id");
		return toDTO(getProjectOrThrow(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProjectDTO> getProjectsByOwner(Long ownerId) {
		validatePositiveId(ownerId, "Owner id");
		return repository.findByOwnerId(ownerId, RECENT_FIRST_SORT).stream().map(this::toDTO).collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProjectDTO> getPublicProjects() {
		return repository.findByVisibilityAndIsArchivedFalse(Visibility.PUBLIC, DISCOVERY_SORT).stream()
				.map(this::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProjectDTO> searchProjects(String name) {
		if (!StringUtils.hasText(name)) {
			throw new InvalidProjectRequestException("Search name is required");
		}
		return repository.searchDiscoverableByName(name.trim(), Visibility.PUBLIC).stream()
				.map(this::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProjectDTO> getProjectsByMember(Long userId) {
		validatePositiveId(userId, "Member user id");
		return repository.findByMemberUserIdOrderByUpdatedAtDesc(userId).stream()
				.map(this::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public ProjectDTO updateProject(Long id, ProjectDTO dto) {
		validatePositiveId(id, "Project id");
		validateProject(dto, false);

		Project project = getProjectOrThrow(id);
		applyMutableFields(project, dto, false);
		return toDTO(repository.save(project));
	}

	@Override
	public void archiveProject(Long id) {
		validatePositiveId(id, "Project id");
		Project project = getProjectOrThrow(id);
		project.setArchived(true);
		repository.save(project);
	}

	@Override
	public void deleteProject(Long id) {
		validatePositiveId(id, "Project id");
		if (!repository.existsById(id)) {
			throw new ResourceNotFoundException("Project not found with id " + id);
		}
		repository.deleteById(id);
	}

	@Override
	public ProjectDTO forkProject(Long id, Long newOwnerId) {
		validatePositiveId(id, "Project id");
		validatePositiveId(newOwnerId, "New owner id");

		Project original = getProjectOrThrow(id);
		if (original.isArchived()) {
			throw new InvalidProjectRequestException("Archived projects cannot be forked");
		}

		Project fork = new Project();
		fork.setOwnerId(newOwnerId);
		fork.setName(original.getName() + "-fork");
		fork.setDescription(original.getDescription());
		fork.setLanguage(original.getLanguage());
		fork.setVisibility(original.getVisibility());
		fork.setTemplateId(original.getTemplateId());
		fork.setArchived(false);
		fork.setMemberUserIds(null);

		original.setForkCount(original.getForkCount() + 1);
		repository.save(original);
		return toDTO(repository.save(fork));
	}

	@Override
	public void starProject(Long id) {
		validatePositiveId(id, "Project id");
		Project project = getProjectOrThrow(id);
		if (project.isArchived()) {
			throw new InvalidProjectRequestException("Archived projects cannot be starred");
		}

		project.setStarCount(project.getStarCount() + 1);
		repository.save(project);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProjectDTO> getProjectsByLanguage(String language) {
		if (!StringUtils.hasText(language)) {
			throw new InvalidProjectRequestException("Language is required");
		}
		return repository.findByLanguageIgnoreCaseAndVisibilityAndIsArchivedFalse(language.trim(), Visibility.PUBLIC,
				DISCOVERY_SORT).stream().map(this::toDTO).collect(Collectors.toList());
	}

	@Override
	public void addMember(Long projectId, Long userId) {
		validatePositiveId(projectId, "Project id");
		validatePositiveId(userId, "Member user id");

		Project project = getProjectOrThrow(projectId);
		if (project.isArchived()) {
			throw new InvalidProjectRequestException("Archived projects cannot be modified");
		}
		project.getMemberUserIds().add(userId);
		repository.save(project);
	}

	@Override
	public void removeMember(Long projectId, Long userId) {
		validatePositiveId(projectId, "Project id");
		validatePositiveId(userId, "Member user id");

		Project project = getProjectOrThrow(projectId);
		if (project.getOwnerId().equals(userId)) {
			throw new InvalidProjectRequestException("Project owner cannot be removed from members");
		}
		project.getMemberUserIds().remove(userId);
		repository.save(project);
	}

	@Override
	@Transactional(readOnly = true)
	public Set<Long> getProjectMembers(Long projectId) {
		validatePositiveId(projectId, "Project id");
		return new LinkedHashSet<>(getProjectOrThrow(projectId).getMemberUserIds());
	}

	private ProjectDTO toDTO(Project p) {
		ProjectDTO dto = new ProjectDTO();
		dto.setProjectId(p.getProjectId());
		dto.setOwnerId(p.getOwnerId());
		dto.setName(p.getName());
		dto.setDescription(p.getDescription());
		dto.setLanguage(p.getLanguage());
		dto.setVisibility(p.getVisibility());
		dto.setTemplateId(p.getTemplateId());
		dto.setArchived(p.isArchived());
		dto.setCreatedAt(p.getCreatedAt());
		dto.setUpdatedAt(p.getUpdatedAt());
		dto.setStarCount(p.getStarCount());
		dto.setForkCount(p.getForkCount());
		dto.setMemberUserIds(p.getMemberUserIds());
		return dto;
	}

	private Project getProjectOrThrow(Long projectId) {
		return repository.findById(projectId)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));
	}

	private void validatePositiveId(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new InvalidProjectRequestException(fieldName + " must be greater than 0");
		}
	}

	private void validateProject(ProjectDTO dto, boolean requireOwnerId) {
		if (dto == null) {
			throw new InvalidProjectRequestException("Project payload is required");
		}
		if (requireOwnerId && dto.getOwnerId() == null) {
			throw new InvalidProjectRequestException("Owner id is required");
		}
		if (requireOwnerId && dto.getOwnerId() != null) {
			validatePositiveId(dto.getOwnerId(), "Owner id");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new InvalidProjectRequestException("Project name is required");
		}
		if (dto.getVisibility() == null) {
			throw new InvalidProjectRequestException("Visibility is required");
		}
		if (dto.getTemplateId() != null && dto.getTemplateId() <= 0) {
			throw new InvalidProjectRequestException("Template id must be greater than 0");
		}
		if (dto.getMemberUserIds() != null && dto.getMemberUserIds().stream().anyMatch(id -> id == null || id <= 0)) {
			throw new InvalidProjectRequestException("Member user ids must contain only positive values");
		}
	}

	private void applyMutableFields(Project project, ProjectDTO dto, boolean includeOwner) {
		if (includeOwner) {
			project.setOwnerId(dto.getOwnerId());
		}
		project.setName(dto.getName().trim());
		project.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
		project.setLanguage(StringUtils.hasText(dto.getLanguage()) ? dto.getLanguage().trim() : null);
		project.setVisibility(dto.getVisibility());
		project.setTemplateId(dto.getTemplateId());
		project.setMemberUserIds(dto.getMemberUserIds());
	}
}
