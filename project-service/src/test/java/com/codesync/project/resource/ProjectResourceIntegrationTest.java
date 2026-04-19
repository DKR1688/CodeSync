package com.codesync.project.resource;

import com.codesync.project.entity.Project;
import com.codesync.project.enums.Visibility;
import com.codesync.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectResourceIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProjectRepository projectRepository;

	@BeforeEach
	void setUp() {
		projectRepository.deleteAll();
	}

	@Test
	void createProjectAcceptsJsonPayload() throws Exception {
		String requestBody = """
				{
				  "ownerId": 1,
				  "name": "My Project",
				  "description": "Test project",
				  "language": "Java",
				  "visibility": "PUBLIC"
				}
				""";

		mockMvc.perform(post("/api/v1/projects")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ownerId").value(1))
				.andExpect(jsonPath("$.name").value("My Project"))
				.andExpect(jsonPath("$.visibility").value("PUBLIC"))
				.andExpect(jsonPath("$.starCount").value(0))
				.andExpect(jsonPath("$.forkCount").value(0))
				.andExpect(jsonPath("$.isArchived").value(false));
	}

	@Test
	void createProjectReturnsHelpfulBadRequestWhenOwnerIdMissing() throws Exception {
		String requestBody = """
				{
				  "name": "My Project",
				  "description": "Test project",
				  "language": "Java",
				  "visibility": "PUBLIC"
				}
				""";

		mockMvc.perform(post("/api/v1/projects")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Owner id is required"));
	}

	@Test
	void searchProjectsReturnsOnlyDiscoverableProjectsInPopularityOrder() throws Exception {
		projectRepository.save(project(1L, "Compiler Studio", Visibility.PUBLIC, false, 2, 0));
		projectRepository.save(project(2L, "Compiler Hub", Visibility.PUBLIC, false, 10, 1));
		projectRepository.save(project(3L, "Compiler Secret", Visibility.PRIVATE, false, 100, 5));
		projectRepository.save(project(4L, "Compiler Old", Visibility.PUBLIC, true, 50, 5));

		mockMvc.perform(get("/api/v1/projects/search").param("name", "Compiler"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Compiler Hub"))
				.andExpect(jsonPath("$[1].name").value("Compiler Studio"));
	}

	private Project project(Long ownerId, String name, Visibility visibility, boolean archived, int starCount,
			int forkCount) {
		Project project = new Project();
		project.setOwnerId(ownerId);
		project.setName(name);
		project.setDescription(name + " description");
		project.setLanguage("Java");
		project.setVisibility(visibility);
		project.setArchived(archived);
		project.setStarCount(starCount);
		project.setForkCount(forkCount);
		return project;
	}
}
