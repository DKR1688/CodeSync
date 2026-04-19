package com.codesync.project.resource;

import com.codesync.project.dto.ProjectDTO;
import com.codesync.project.enums.Visibility;
import com.codesync.project.exception.InvalidProjectRequestException;
import com.codesync.project.security.AuthenticatedUser;
import com.codesync.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@Validated
@RequestMapping("/api/v1/projects")
public class ProjectResource {

	private final ProjectService service;

	public ProjectResource(ProjectService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectDTO dto, Authentication authentication) {
		dto.setOwnerId(requireCurrentUserId(authentication));
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createProject(dto));
	}

	@GetMapping("/{id}")
	public ProjectDTO getProjectById(@PathVariable Long id, Authentication authentication) {
		ProjectDTO project = service.getProjectById(id);
		assertCanViewProject(project, authentication);
		return project;
	}

	@GetMapping("/owner/{ownerId}")
	public List<ProjectDTO> getProjectsByOwner(@PathVariable Long ownerId, Authentication authentication) {
		return service.getProjectsByOwner(ownerId).stream()
				.filter(project -> canViewProject(project, authentication))
				.toList();
	}

	@GetMapping("/public")
	public List<ProjectDTO> getPublicProjects() {
		return service.getPublicProjects();
	}

	@GetMapping("/search")
	public List<ProjectDTO> searchProjects(@RequestParam String name) {
		return service.searchProjects(name);
	}

	@GetMapping("/member/{userId}")
	public List<ProjectDTO> getProjectsByMember(@PathVariable Long userId, Authentication authentication) {
		assertSameUserOrAdmin(userId, authentication);
		return service.getProjectsByMember(userId);
	}

	@GetMapping("/language/{lang}")
	public List<ProjectDTO> getProjectsByLanguage(@PathVariable String lang) {
		return service.getProjectsByLanguage(lang);
	}

	@GetMapping("/{id}/members")
	public Set<Long> getProjectMembers(@PathVariable Long id, Authentication authentication) {
		ProjectDTO project = service.getProjectById(id);
		assertCanViewProject(project, authentication);
		return service.getProjectMembers(id);
	}

	@PutMapping("/{id}")
	public ProjectDTO updateProject(@PathVariable Long id, @Valid @RequestBody ProjectDTO dto,
			Authentication authentication) {
		ProjectDTO existingProject = service.getProjectById(id);
		assertOwnerOrAdmin(existingProject, authentication);
		dto.setOwnerId(existingProject.getOwnerId());
		return service.updateProject(id, dto);
	}

	@PutMapping("/{id}/archive")
	public ResponseEntity<Void> archiveProject(@PathVariable Long id, Authentication authentication) {
		assertOwnerOrAdmin(service.getProjectById(id), authentication);
		service.archiveProject(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}/star")
	public ResponseEntity<Void> starProject(@PathVariable Long id, Authentication authentication) {
		ProjectDTO project = service.getProjectById(id);
		assertCanViewProject(project, authentication);
		requireCurrentUserId(authentication);
		service.starProject(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/fork/{userId}")
	public ResponseEntity<ProjectDTO> forkProject(@PathVariable Long id, @PathVariable Long userId,
			Authentication authentication) {
		Long currentUserId = requireCurrentUserId(authentication);
		if (!currentUserId.equals(userId)) {
			throw new AccessDeniedException("You can only fork projects into your own account");
		}
		ProjectDTO source = service.getProjectById(id);
		if (source.getVisibility() != Visibility.PUBLIC && !isAdmin(authentication)) {
			throw new AccessDeniedException("Only public projects can be forked");
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(service.forkProject(id, currentUserId));
	}

	@PostMapping("/{id}/members/{userId}")
	public ResponseEntity<Void> addMember(@PathVariable Long id, @PathVariable Long userId, Authentication authentication) {
		assertOwnerOrAdmin(service.getProjectById(id), authentication);
		service.addMember(id, userId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}/members/{userId}")
	public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId,
			Authentication authentication) {
		assertOwnerOrAdmin(service.getProjectById(id), authentication);
		service.removeMember(id, userId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
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
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("Authentication is required");
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof AuthenticatedUser user) {
			return user.userId();
		}
		String name = authentication.getName();
		try {
			return Long.valueOf(name);
		} catch (NumberFormatException ex) {
			throw new InvalidProjectRequestException("Unable to determine the authenticated user");
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
