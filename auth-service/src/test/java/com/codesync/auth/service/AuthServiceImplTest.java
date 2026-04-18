package com.codesync.auth.service;

import com.codesync.auth.dto.UpdateProfileRequest;
import com.codesync.auth.entity.User;
import com.codesync.auth.exception.AuthException;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setUserId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(encoder.encode("password123"));
        testUser.setFullName("Test User");
        testUser.setRole("DEVELOPER");
        testUser.setActive(true);
    }

    @Test
    void register_ShouldSaveUser() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = authService.register(testUser);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(AuthException.class, () -> authService.register(testUser));
    }

    @Test
    void login_ShouldReturnToken_WhenValidCredentials() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt.token");

        String token = authService.login("test@example.com", "password123");

        assertEquals("jwt.token", token);
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthException.class, () -> authService.login("test@example.com", "password123"));
    }

    @Test
    void login_ShouldThrowException_WhenInvalidPassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(AuthException.class, () -> authService.login("test@example.com", "wrongpass"));
    }

    @Test
    void login_ShouldThrowException_WhenAccountDeactivated() {
        testUser.setActive(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(AuthException.class, () -> authService.login("test@example.com", "password123"));
    }

    @Test
    void updateProfile_ShouldUpdateUser() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Name");

        when(userRepository.findByUserId(1)).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = authService.updateProfile(1, request, "test@example.com");

        assertEquals("New Name", result.getFullName());
    }

    @Test
    void updateProfile_ShouldThrowException_WhenUnauthorized() {
        when(userRepository.findByUserId(1)).thenReturn(testUser);

        assertThrows(AuthException.class, () -> authService.updateProfile(1, new UpdateProfileRequest(), "other@example.com"));
    }

    @Test
    void changePassword_ShouldUpdatePassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        authService.changePassword("test@example.com", "password123", "newpass123");

        verify(userRepository).save(any(User.class));
    }

    @Test
    void changePassword_ShouldThrowException_WhenWrongCurrentPassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(AuthException.class, () -> authService.changePassword("test@example.com", "wrongpass", "newpass"));
    }

    @Test
    void searchUsers_ShouldReturnUsers() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("test", "test")).thenReturn(users);

        List<User> result = authService.searchUsers("test");

        assertEquals(1, result.size());
    }

    @Test
    void searchUsers_ShouldReturnAllUsers_WhenQueryBlank() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = authService.searchUsers("");

        assertEquals(1, result.size());
    }

    @Test
    void refreshToken_ShouldReturnNewToken() {
        when(jwtUtil.extractEmail("old.token")).thenReturn("test@example.com");
        when(jwtUtil.generateToken("test@example.com")).thenReturn("new.token");

        String result = authService.refreshToken("old.token");

        assertEquals("new.token", result);
    }

    @Test
    void deactivateAccount_ShouldDeactivateUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = authService.deactivateAccount("test@example.com");

        assertFalse(result.isActive());
    }

    @Test
    void getUserById_ShouldReturnUser() {
        when(userRepository.findByUserId(1)).thenReturn(testUser);

        User result = authService.getUserById(1);

        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserByEmail_ShouldReturnUser() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        User result = authService.getUserByEmail("test@example.com");

        assertNotNull(result);
    }
}