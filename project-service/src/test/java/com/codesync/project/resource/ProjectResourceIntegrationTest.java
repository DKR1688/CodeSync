package com.codesync.project.resource;

import com.codesync.project.entity.Project;
import com.codesync.project.enums.Visibility;
import com.codesync.project.repository.ProjectRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
	void createProjectUsesAuthenticatedUserAsOwner() throws Exception {
		String requestBody = """
				{
				  "name": "My Project",
				  "description": "Test project",
				  "language": "Java",
				  "visibility": "PUBLIC"
				}
				""";

		mockMvc.perform(post("/api/v1/projects")
						.header("Authorization", bearerToken(1L, "DEVELOPER"))
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
	void createProjectRequiresAuthentication() throws Exception {
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
				.andExpect(status().isUnauthorized());
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

	@Test
	void privateProjectsCannotBeViewedByGuests() throws Exception {
		Project saved = projectRepository.save(project(3L, "Compiler Secret", Visibility.PRIVATE, false, 0, 0));

		mockMvc.perform(get("/api/v1/projects/{id}", saved.getProjectId()))
				.andExpect(status().isForbidden());
	}

	@Test
	void authenticatedUserCanManageOwnProjectLifecycle() throws Exception {
		Project saved = projectRepository.save(project(7L, "Workspace", Visibility.PUBLIC, false, 0, 0));
		String ownerToken = bearerToken(7L, "DEVELOPER");

		mockMvc.perform(put("/api/v1/projects/{id}", saved.getProjectId())
						.header("Authorization", ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Workspace Updated",
								  "description": "Updated description",
								  "language": "Kotlin",
								  "visibility": "PRIVATE",
								  "memberUserIds": [9]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Workspace Updated"))
				.andExpect(jsonPath("$.visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.memberUserIds[0]").value(9));

		mockMvc.perform(post("/api/v1/projects/{id}/members/{userId}", saved.getProjectId(), 11L)
						.header("Authorization", ownerToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/projects/{id}/members", saved.getProjectId())
						.header("Authorization", ownerToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());

		mockMvc.perform(put("/api/v1/projects/{id}/star", saved.getProjectId())
						.header("Authorization", bearerToken(9L, "DEVELOPER")))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/projects/{id}/fork/{userId}", saved.getProjectId(), 12L)
						.header("Authorization", bearerToken(12L, "DEVELOPER")))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/v1/projects/{id}", saved.getProjectId())
						.header("Authorization", ownerToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Workspace Public",
								  "description": "Updated description",
								  "language": "Kotlin",
								  "visibility": "PUBLIC",
								  "memberUserIds": [9, 11]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.visibility").value("PUBLIC"));

		mockMvc.perform(post("/api/v1/projects/{id}/fork/{userId}", saved.getProjectId(), 12L)
						.header("Authorization", bearerToken(12L, "DEVELOPER")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ownerId").value(12))
				.andExpect(jsonPath("$.name").value("Workspace Public-fork"));

		mockMvc.perform(put("/api/v1/projects/{id}/archive", saved.getProjectId())
						.header("Authorization", ownerToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/v1/projects/{id}", saved.getProjectId())
						.header("Authorization", ownerToken))
				.andExpect(status().isNoContent());
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

	private String bearerToken(Long userId, String role) throws Exception {
		byte[] secretBytes = MessageDigest.getInstance("SHA-512")
				.digest("test-secret-that-is-long-enough-for-jwt-signing".getBytes(StandardCharsets.UTF_8));
		String token = Jwts.builder()
				.setSubject("user" + userId + "@example.com")
				.claim("userId", userId)
				.claim("role", role)
				.setIssuedAt(Date.from(Instant.now()))
				.setExpiration(Date.from(Instant.now().plusSeconds(3600)))
				.signWith(Keys.hmacShaKeyFor(secretBytes), SignatureAlgorithm.HS256)
				.compact();
		return "Bearer " + token;
	}
}
