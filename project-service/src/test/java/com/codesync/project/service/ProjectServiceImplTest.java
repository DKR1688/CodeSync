package com.codesync.project.service;

import com.codesync.project.dto.ProjectDTO;
import com.codesync.project.entity.Project;
import com.codesync.project.enums.Visibility;
import com.codesync.project.exception.InvalidProjectRequestException;
import com.codesync.project.exception.ResourceNotFoundException;
import com.codesync.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

	@Mock
	private ProjectRepository repository;

	@InjectMocks
	private ProjectServiceImpl service;

	private ProjectDTO projectDTO;
	private Project project;

	@BeforeEach
	void setUp() {
		projectDTO = new ProjectDTO();
		projectDTO.setOwnerId(10L);
		projectDTO.setName("Compiler");
		projectDTO.setDescription("Core project");
		projectDTO.setLanguage("Java");
		projectDTO.setVisibility(Visibility.PUBLIC);
		projectDTO.setTemplateId(20L);
		projectDTO.setMemberUserIds(Set.of(11L, 12L));

		project = new Project();
		project.setProjectId(1L);
		project.setOwnerId(10L);
		project.setName("Compiler");
		project.setDescription("Core project");
		project.setLanguage("Java");
		project.setVisibility(Visibility.PUBLIC);
		project.setTemplateId(20L);
		project.setMemberUserIds(new LinkedHashSet<>(Set.of(11L, 12L)));
	}

	@Test
	void createProjectInitializesManagedFields() {
		when(repository.save(any(Project.class))).thenAnswer(invocation -> {
			Project saved = invocation.getArgument(0);
			saved.setProjectId(1L);
			return saved;
		});

		ProjectDTO created = service.createProject(projectDTO);

		assertEquals(1L, created.getProjectId());
		assertEquals(0, created.getStarCount());
		assertEquals(0, created.getForkCount());
		assertFalse(created.isArchived());
		assertEquals(Set.of(11L, 12L), created.getMemberUserIds());
	}

	@Test
	void createProjectRejectsMissingOwnerId() {
		projectDTO.setOwnerId(null);

		assertThrows(InvalidProjectRequestException.class, () -> service.createProject(projectDTO));
		verify(repository, never()).save(any(Project.class));
	}

	@Test
	void getProjectsByMemberUsesRepositoryQuery() {
		when(repository.findByMemberUserIdOrderByUpdatedAtDesc(11L)).thenReturn(List.of(project));

		List<ProjectDTO> projects = service.getProjectsByMember(11L);

		assertEquals(1, projects.size());
		assertEquals("Compiler", projects.getFirst().getName());
	}

	@Test
	void updateProjectPreservesOwnerAndManagedCounters() {
		project.setStarCount(5);
		project.setForkCount(2);
		when(repository.findById(1L)).thenReturn(Optional.of(project));
		when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProjectDTO update = new ProjectDTO();
		update.setName("Compiler 2");
		update.setDescription("Updated");
		update.setLanguage("Kotlin");
		update.setVisibility(Visibility.PRIVATE);
		update.setTemplateId(21L);
		update.setMemberUserIds(Set.of(15L));

		ProjectDTO saved = service.updateProject(1L, update);

		assertEquals(10L, saved.getOwnerId());
		assertEquals(5, saved.getStarCount());
		assertEquals(2, saved.getForkCount());
		assertEquals(Set.of(15L), saved.getMemberUserIds());
	}

	@Test
	void deleteProjectThrowsWhenMissing() {
		when(repository.existsById(99L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> service.deleteProject(99L));
	}

	@Test
	void archiveProjectMarksProjectArchived() {
		when(repository.findById(1L)).thenReturn(Optional.of(project));
		when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.archiveProject(1L);

		assertTrue(project.isArchived());
		verify(repository).save(project);
	}

	@Test
	void starProjectIncrementsCounter() {
		when(repository.findById(1L)).thenReturn(Optional.of(project));
		when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.starProject(1L);

		assertEquals(1, project.getStarCount());
		verify(repository).save(project);
	}

	@Test
	void starProjectRejectsArchivedProject() {
		project.setArchived(true);
		when(repository.findById(1L)).thenReturn(Optional.of(project));

		assertThrows(InvalidProjectRequestException.class, () -> service.starProject(1L));
		verify(repository, never()).save(any(Project.class));
	}

	@Test
	void forkProjectCreatesNewProjectAndIncrementsSourceForkCount() {
		when(repository.findById(1L)).thenReturn(Optional.of(project));
		when(repository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProjectDTO forked = service.forkProject(1L, 99L);
		ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);

		verify(repository, times(2)).save(captor.capture());
		List<Project> savedProjects = captor.getAllValues();

		assertEquals(1, project.getForkCount());
		assertEquals("Compiler-fork", forked.getName());
		assertEquals(99L, forked.getOwnerId());
		assertEquals(20L, forked.getTemplateId());
		assertTrue(savedProjects.stream().anyMatch(saved -> Objects.equals(saved.getOwnerId(), 99L)));
	}

	@Test
	void forkProjectRejectsArchivedSourceProject() {
		project.setArchived(true);
		when(repository.findById(1L)).thenReturn(Optional.of(project));

		assertThrows(InvalidProjectRequestException.class, () -> service.forkProject(1L, 99L));
		verify(repository, never()).save(any(Project.class));
	}

	@Test
	void getProjectByIdThrowsWhenMissing() {
		when(repository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.getProjectById(123L));
	}
}
