package com.codesync.project.resource;

import com.codesync.project.client.AuthServiceClient;
import com.codesync.project.client.FileServiceClient;
import com.codesync.project.dto.ProjectDTO;
import com.codesync.project.dto.ProjectPermissionDTO;
import com.codesync.project.enums.Visibility;
import com.codesync.project.exception.InvalidProjectRequestException;
import com.codesync.project.security.AuthenticatedUser;
import com.codesync.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

@RestController
@Validated
@RequestMapping("/api/v1/projects")
public class ProjectResource {

	private final ProjectService service;
	private final AuthServiceClient authServiceClient;
	private final FileServiceClient fileServiceClient;

	public ProjectResource(ProjectService service, AuthServiceClient authServiceClient,
			FileServiceClient fileServiceClient) {
		this.service = service;
		this.authServiceClient = authServiceClient;
		this.fileServiceClient = fileServiceClient;
	}

	@PostMapping
	@Operation(summary = "Create project", tags = { "01. Create Project" })
	public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectDTO dto, Authentication authentication) {
		dto.setOwnerId(requireCurrentUserId(authentication));
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createProject(dto));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Project by id", tags = { "02. Get Project By Id" })
	public ProjectDTO getProjectById(@PathVariable Long id, Authentication authentication) {
		ProjectDTO project = service.getProjectById(id);
		assertCanViewProject(project, authentication);
		return project;
	}

	@GetMapping("/{id}/permissions")
	@Operation(summary = "Project permissions", tags = { "03. Get Project Permissions" })
	public ProjectPermissionDTO getProjectPermissions(@PathVariable Long id, Authentication authentication) {
		ProjectDTO project = service.getProjectById(id);
		try {
			Long currentUserId = resolveCurrentUserId(authentication);
			boolean admin = isAdmin(authentication);
			Long ownerId = project.getOwnerId();
			Set<Long> memberUserIds = project.getMemberUserIds() == null
					? new LinkedHashSet<>()
					: new LinkedHashSet<>(project.getMemberUserIds());
			Visibility visibility = project.getVisibility() == null ? Visibility.PRIVATE : project.getVisibility();
			boolean archived = project.isArchived();
			boolean owner = currentUserId != null && ownerId != null && ownerId.equals(currentUserId);
			boolean member = currentUserId != null && memberUserIds.contains(currentUserId);
			boolean canRead = visibility == Visibility.PUBLIC || admin || owner || member;
			boolean canWrite = !archived && (admin || owner || member);
			boolean canManage = admin || owner;

			return new ProjectPermissionDTO(
					project.getProjectId() != null ? project.getProjectId() : id,
					canRead,
					canWrite,
					canManage,
					owner,
					member,
					admin,
					archived,
					visibility);
		} catch (RuntimeException ex) {
			if (project.getVisibility() == Visibility.PUBLIC) {
				return new ProjectPermissionDTO(
						project.getProjectId() != null ? project.getProjectId() : id,
						true,
						false,
						false,
						false,
						false,
						false,
						project.isArchived(),
						project.getVisibility());
			}
			throw ex;
		}
	}

	@GetMapping("/owner/{ownerId}")
	@Operation(summary = "Projects by owner", tags = { "07. Get Projects By Owner" })
	public List<ProjectDTO> getProjectsByOwner(@PathVariable Long ownerId, Authentication authentication) {
		return service.getProjectsByOwner(ownerId).stream()
				.filter(project -> canViewProject(project, authentication))
				.toList();
	}

	@GetMapping("/public")
	@Operation(summary = "Public projects", tags = { "04. Get Public Projects" })
	public List<ProjectDTO> getPublicProjects() {
		return service.getPublicProjects();
	}

	@GetMapping("/admin/all")
	@Operation(summary = "All projects", tags = { "17. Get All Projects" })
	public List<ProjectDTO> getAllProjects(Authentication authentication) {
		if (!isAdmin(authentication)) {
			throw new AccessDeniedException("Administrator access is required");
		}
		return service.getAllProjects();
	}

	@GetMapping("/search")
	@Operation(summary = "Search projects", tags = { "05. Search Projects" })
	public List<ProjectDTO> searchProjects(@RequestParam(required = false) String name,
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String ownerUsername) {
		String searchValue = StringUtils.hasText(name) ? name : keyword;
		boolean hasNameFilter = StringUtils.hasText(searchValue);
		boolean hasOwnerFilter = StringUtils.hasText(ownerUsername);
		if (!hasNameFilter && !hasOwnerFilter) {
			throw new InvalidProjectRequestException("At least one search filter is required");
		}

		List<ProjectDTO> results = hasNameFilter ? service.searchProjects(searchValue) : service.getPublicProjects();
		if (!hasOwnerFilter) {
			return results;
		}

		Set<Long> ownerIds = new LinkedHashSet<>(authServiceClient.searchUserIdsByUsername(ownerUsername));
		if (ownerIds.isEmpty()) {
			return List.of();
		}

		Set<Long> allowedProjectIds = ownerIds.stream()
				.flatMap(ownerId -> service.getProjectsByOwner(ownerId).stream())
				.filter(this::isDiscoverableProject)
				.map(ProjectDTO::getProjectId)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

		return results.stream()
				.filter(project -> allowedProjectIds.contains(project.getProjectId()))
				.toList();
	}

	@GetMapping("/member/{userId}")
	@Operation(summary = "Projects by member", tags = { "08. Get Projects By Member" })
	public List<ProjectDTO> getProjectsByMember(@PathVariable Long userId, Authentication authentication) {
		assertSameUserOrAdmin(userId, authentication);
		return service.getProjectsByMember(userId);
	}

	@GetMapping("/language/{lang}")
	@Operation(summary = "Projects by language", tags = { "06. Get Projects By Language" })
	public List<ProjectDTO> getProjectsByLanguage(@PathVariable String lang) {
		return service.getProjectsByLanguage(lang);
	}

	@GetMapping("/{id}/members")
	@Operation(summary = "Project members", tags = { "09. Get Project Members" })
	public Set<Long> getProjectMembers(@PathVariable Long id, Authentication authentication) {
		ProjectDTO project = service.getProjectById(id);
		assertCanViewProject(project, authentication);
		return service.getProjectMembers(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update project", tags = { "10. Update Project" })
	public ProjectDTO updateProject(@PathVariable Long id, @Valid @RequestBody ProjectDTO dto,
			Authentication authentication) {
		ProjectDTO existingProject = service.getProjectById(id);
		assertOwnerOrAdmin(existingProject, authentication);
		dto.setOwnerId(existingProject.getOwnerId());
		return service.updateProject(id, dto);
	}

	@PutMapping("/{id}/archive")
	@Operation(summary = "Archive project", tags = { "15. Archive Project" })
	public ResponseEntity<Void> archiveProject(@PathVariable Long id, Authentication authentication) {
		assertOwnerOrAdmin(service.getProjectById(id), authentication);
		service.archiveProject(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}/star")
	@Operation(summary = "Star project", tags = { "13. Star Project" })
	public ResponseEntity<Void> starProject(@PathVariable Long id, Authentication authentication) {
		ProjectDTO project = service.getProjectById(id);
		assertCanViewProject(project, authentication);
		requireCurrentUserId(authentication);
		service.starProject(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/fork/{userId}")
	@Operation(summary = "Fork project", tags = { "14. Fork Project" })
	public ResponseEntity<ProjectDTO> forkProject(@PathVariable Long id, @PathVariable Long userId,
			Authentication authentication,
			@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		Long currentUserId = requireCurrentUserId(authentication);
		if (!currentUserId.equals(userId)) {
			throw new AccessDeniedException("You can only fork projects into your own account");
		}
		ProjectDTO source = service.getProjectById(id);
		if (source.getVisibility() != Visibility.PUBLIC && !isAdmin(authentication)) {
			throw new AccessDeniedException("Only public projects can be forked");
		}
		ProjectDTO forkedProject = service.forkProject(id, currentUserId);
		try {
			fileServiceClient.copyProjectFiles(id, forkedProject.getProjectId(), authorizationHeader);
		} catch (RuntimeException ex) {
			service.rollbackFork(id, forkedProject.getProjectId());
			throw ex;
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(forkedProject);
	}

	@PostMapping("/{id}/members/{userId}")
	@Operation(summary = "Add member", tags = { "11. Add Member" })
	public ResponseEntity<Void> addMember(@PathVariable Long id, @PathVariable Long userId, Authentication authentication) {
		assertOwnerOrAdmin(service.getProjectById(id), authentication);
		authServiceClient.assertUserExists(userId);
		service.addMember(id, userId, requireCurrentUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}/members/{userId}")
	@Operation(summary = "Remove member", tags = { "12. Remove Member" })
	public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId,
			Authentication authentication) {
		assertOwnerOrAdmin(service.getProjectById(id), authentication);
		service.removeMember(id, userId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete project", tags = { "16. Delete Project" })
	public ResponseEntity<Void> deleteProject(@PathVariable Long id, Authentication authentication) {
		assertOwnerOrAdmin(service.getProjectById(id), authentication);
		service.deleteProject(id);
		return ResponseEntity.noContent().build();
	}

	private void assertCanViewProject(ProjectDTO project, Authentication authentication) {
		if (!canViewProject(project, authentication)) {
			throw new AccessDeniedException("You do not have access to this project");
		}
	}

	private boolean canViewProject(ProjectDTO project, Authentication authentication) {
		if (project.getVisibility() == Visibility.PUBLIC) {
			return true;
		}
		if (authentication == null) {
			return false;
		}
		Long currentUserId = requireCurrentUserId(authentication);
		return isAdmin(authentication)
				|| project.getOwnerId().equals(currentUserId)
				|| project.getMemberUserIds().contains(currentUserId);
	}

	private boolean isDiscoverableProject(ProjectDTO project) {
		return project.getVisibility() == Visibility.PUBLIC && !project.isArchived();
	}

	private void assertOwnerOrAdmin(ProjectDTO project, Authentication authentication) {
		Long currentUserId = requireCurrentUserId(authentication);
		if (!isAdmin(authentication) && !project.getOwnerId().equals(currentUserId)) {
			throw new AccessDeniedException("Only the project owner or an admin can perform this action");
		}
	}

	private void assertSameUserOrAdmin(Long expectedUserId, Authentication authentication) {
		Long currentUserId = requireCurrentUserId(authentication);
		if (!isAdmin(authentication) && !expectedUserId.equals(currentUserId)) {
			throw new AccessDeniedException("You can only access your own member projects");
		}
	}

	private Long requireCurrentUserId(Authentication authentication) {
		Long currentUserId = resolveCurrentUserId(authentication);
		if (currentUserId == null) {
			throw new AccessDeniedException("Authentication is required");
		}
		return currentUserId;
	}

	private Long resolveCurrentUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof AuthenticatedUser user) {
			return user.userId();
		}
		String name = authentication.getName();
		try {
			return Long.valueOf(name);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private boolean isAdmin(Authentication authentication) {
		Object principal = authentication != null ? authentication.getPrincipal() : null;
		if (principal instanceof AuthenticatedUser user) {
			return user.isAdmin();
		}
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
	}
}
