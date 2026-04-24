package com.codesync.project.resource;

import com.codesync.project.client.AuthServiceClient;
import com.codesync.project.client.FileServiceClient;
import com.codesync.project.dto.ProjectDTO;
import com.codesync.project.enums.Visibility;
import com.codesync.project.exception.InvalidProjectRequestException;
import com.codesync.project.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectResourceTest {

	@Mock
	private ProjectService service;

	@Mock
	private AuthServiceClient authServiceClient;

	@Mock
	private FileServiceClient fileServiceClient;

	private ProjectResource resource;

	@BeforeEach
	void setUp() {
		resource = new ProjectResource(service, authServiceClient, fileServiceClient);
	}

	@Test
	void searchProjectsFiltersPublicResultsByOwnerUsername() {
		ProjectDTO nameMatched = project(1L, 10L, "Compiler", Visibility.PUBLIC, false);
		ProjectDTO sameOwner = project(2L, 10L, "Compiler Utils", Visibility.PUBLIC, false);
		ProjectDTO archived = project(3L, 10L, "Compiler Legacy", Visibility.PUBLIC, true);

		when(service.searchProjects("Compiler")).thenReturn(List.of(nameMatched, sameOwner));
		when(authServiceClient.searchUserIdsByUsername("alice")).thenReturn(List.of(10L));
		when(service.getProjectsByOwner(10L)).thenReturn(List.of(nameMatched, sameOwner, archived));

		List<ProjectDTO> results = resource.searchProjects("Compiler", null, "alice");

		assertEquals(List.of(1L, 2L), results.stream().map(ProjectDTO::getProjectId).toList());
		verify(service).searchProjects("Compiler");
		verify(authServiceClient).searchUserIdsByUsername("alice");
	}

	@Test
	void searchProjectsRejectsMissingFilters() {
		assertThrows(InvalidProjectRequestException.class, () -> resource.searchProjects(null, null, null));
	}

	private ProjectDTO project(Long projectId, Long ownerId, String name, Visibility visibility, boolean archived) {
		ProjectDTO dto = new ProjectDTO();
		dto.setProjectId(projectId);
		dto.setOwnerId(ownerId);
		dto.setName(name);
		dto.setLanguage("Java");
		dto.setVisibility(visibility);
		dto.setArchived(archived);
		return dto;
	}
}
