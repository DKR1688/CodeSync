package com.codesync.auth.security;

import com.codesync.auth.entity.User;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OAuth2LoginSuccessHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new OAuth2LoginSuccessHandler(authService, userRepository);
    }

    @Test
    void onAuthenticationSuccess_ShouldHandleGitHubUser() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(Map.of(
            "login", "githubuser",
            "email", "github@example.com",
            "name", "GitHub User"
        ));
        when(userRepository.findByEmail("github@example.com")).thenReturn(Optional.empty());
        when(authService.register(any(User.class))).thenReturn(new User());

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).findByEmail("github@example.com");
        verify(authService).register(any(User.class));
        verify(response).sendRedirect(contains("http://localhost:8080/login/oauth2/code/github"));
    }

    @Test
    void onAuthenticationSuccess_ShouldHandleGoogleUser() throws IOException {
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(Map.of(
            "email", "google@example.com",
            "name", "Google User"
        ));
        when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(new User()));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).findByEmail("google@example.com");
        verify(authService, never()).register(any(User.class));
        verify(response).sendRedirect(contains("http://localhost:8080/login/oauth2/code/google"));
    }

    @Test
    void onAuthenticationSuccess_ShouldHandleExistingUser() throws IOException {
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(Map.of(
            "email", "existing@example.com",
            "name", "Existing User"
        ));
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).findByEmail("existing@example.com");
        verify(authService, never()).register(any(User.class));
    }
}