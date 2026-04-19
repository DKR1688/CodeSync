package com.codesync.auth.controller;

import com.codesync.auth.dto.AuthResponse;
import com.codesync.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthResourceIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
	}

	@Test
	void registerLoginRefreshAndViewProfileFlowWorks() throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "deepak",
								  "email": "deepak@example.com",
								  "password": "secret123",
								  "fullName": "Deepak Kumar"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").isNumber())
				.andExpect(jsonPath("$.username").value("deepak"));

		String loginResponse = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "deepak@example.com",
								  "password": "secret123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString())
				.andReturn()
				.getResponse()
				.getContentAsString();

		AuthResponse authResponse = objectMapper.readValue(loginResponse, AuthResponse.class);

		mockMvc.perform(post("/auth/refresh")
						.header("Authorization", "Bearer " + authResponse.getToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString());

		int userId = userRepository.findByEmail("deepak@example.com").orElseThrow().getUserId();
		mockMvc.perform(get("/auth/profile/{id}", userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("deepak@example.com"));
	}

	@Test
	void authenticatedUserCanUpdateOwnProfileAndPassword() throws Exception {
		registerUser("owner", "owner@example.com");
		int userId = userRepository.findByEmail("owner@example.com").orElseThrow().getUserId();
		String token = login("owner@example.com", "secret123");

		mockMvc.perform(put("/auth/profile/{id}", userId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "owner2",
								  "email": "owner2@example.com",
								  "fullName": "Owner Updated",
								  "bio": "Building CodeSync"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("owner2"))
				.andExpect(jsonPath("$.email").value("owner2@example.com"));

		mockMvc.perform(put("/auth/password/{id}", userId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "currentPassword": "secret123",
								  "newPassword": "secret456"
								}
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "owner2@example.com",
								  "password": "secret456"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString());
	}

	@Test
	void deactivatedUserCannotLogInAgain() throws Exception {
		registerUser("inactive", "inactive@example.com");
		int userId = userRepository.findByEmail("inactive@example.com").orElseThrow().getUserId();
		String token = login("inactive@example.com", "secret123");

		mockMvc.perform(delete("/auth/deactivate/{id}", userId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "inactive@example.com",
								  "password": "secret123"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Account is deactivated"));
	}

	@Test
	void logoutInvalidatesTokenForRefreshAndProtectedRequests() throws Exception {
		registerUser("logoutuser", "logout@example.com");
		int userId = userRepository.findByEmail("logout@example.com").orElseThrow().getUserId();
		String token = login("logout@example.com", "secret123");

		mockMvc.perform(post("/auth/logout")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Logout successful"));

		mockMvc.perform(post("/auth/refresh")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid token"));

		mockMvc.perform(put("/auth/profile/{id}", userId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Should Not Work"
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userCannotUpdateAnotherUsersProfile() throws Exception {
		registerUser("userone", "userone@example.com");
		registerUser("usertwo", "usertwo@example.com");
		int otherUserId = userRepository.findByEmail("usertwo@example.com").orElseThrow().getUserId();
		String token = login("userone@example.com", "secret123");

		mockMvc.perform(put("/auth/profile/{id}", otherUserId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fullName": "Hacked"
								}
								"""))
				.andExpect(status().isForbidden());
	}

	private void registerUser(String username, String email) throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "email": "%s",
								  "password": "secret123",
								  "fullName": "Test User"
								}
								""".formatted(username, email)))
				.andExpect(status().isOk());
	}

	private String login(String email, String password) throws Exception {
		String loginResponse = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s"
								}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();

		return objectMapper.readValue(loginResponse, AuthResponse.class).getToken();
	}
}
