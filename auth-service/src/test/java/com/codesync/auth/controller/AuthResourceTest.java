package com.codesync.auth.controller;

import com.codesync.auth.dto.*;
import com.codesync.auth.entity.User;
import com.codesync.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthResource.class)
class AuthResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UserResponse testUserResponse;
    private AuthResponse testAuthResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setRole("DEVELOPER");

        testUserResponse = new UserResponse();
        testUserResponse.setUserId(1);
        testUserResponse.setUsername("testuser");
        testUserResponse.setEmail("test@example.com");
        testUserResponse.setFullName("Test User");
        testUserResponse.setRole("DEVELOPER");

        testAuthResponse = new AuthResponse("jwt.token.here", "Success");
    }

    @Test
    void register_ShouldReturnUserResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        Mockito.when(authService.register(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void login_ShouldReturnAuthResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        Mockito.when(authService.login("test@example.com", "password123")).thenReturn("jwt.token.here");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"));
    }

    @Test
    @WithMockUser
    void getProfile_ShouldReturnUserResponse() throws Exception {
        Mockito.when(authService.getUserById(1)).thenReturn(testUser);

        mockMvc.perform(get("/auth/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @WithMockUser
    void updateProfile_ShouldReturnUpdatedUser() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");

        Mockito.when(authService.updateProfile(eq(1), any(UpdateProfileRequest.class), anyString())).thenReturn(testUser);

        mockMvc.perform(put("/auth/profile/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void changePassword_ShouldReturnSuccess() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldpass");
        request.setNewPassword("newpass");

        mockMvc.perform(put("/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @WithMockUser
    void searchUsers_ShouldReturnList() throws Exception {
        List<User> users = Arrays.asList(testUser);
        Mockito.when(authService.searchUsers("test")).thenReturn(users);

        mockMvc.perform(get("/auth/search?query=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    @WithMockUser
    void refreshToken_ShouldReturnNewToken() throws Exception {
        TokenRequest request = new TokenRequest();
        request.setToken("old.token");

        Mockito.when(authService.refreshToken("old.token")).thenReturn("new.token");

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new.token"));
    }

    @Test
    @WithMockUser
    void logout_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    @WithMockUser
    void deactivateAccount_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(put("/auth/deactivate")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deactivated successfully"));
    }
}