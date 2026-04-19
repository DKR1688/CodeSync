package com.codesync.auth.controller;

import com.codesync.auth.dto.AuthResponse;
import com.codesync.auth.dto.ChangePasswordRequest;
import com.codesync.auth.dto.LoginRequest;
import com.codesync.auth.dto.RegisterRequest;
import com.codesync.auth.dto.UpdateProfileRequest;
import com.codesync.auth.dto.UserResponse;
import com.codesync.auth.entity.User;
import com.codesync.auth.exception.AuthException;
import com.codesync.auth.mapper.UserMapper;
import com.codesync.auth.security.AuthenticatedUser;
import com.codesync.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthResource {

	@Autowired
	private AuthService service;

	@PostMapping("/register")
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPasswordHash(request.getPassword());
		user.setFullName(request.getFullName());

		User saved = service.register(user);
		return UserMapper.toDTO(saved);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		String token = service.login(request.getEmail(), request.getPassword());
		return new AuthResponse(token, "Login successful");
	}

	@PostMapping("/logout")
	public AuthResponse logout(@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		service.logout(extractBearerToken(authorizationHeader));
		return new AuthResponse(null, "Logout successful");
	}

	@PostMapping("/refresh")
	public AuthResponse refresh(@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		String token = service.refreshToken(extractBearerToken(authorizationHeader));
		return new AuthResponse(token, "Token refreshed successfully");
	}

	@GetMapping("/profile/{id}")
	public UserResponse getProfile(@PathVariable int id) {
		User user = service.getUserById(id);
		return UserMapper.toDTO(user);
	}

	@PutMapping("/profile/{id}")
	public UserResponse updateProfile(@PathVariable int id, @Valid @RequestBody UpdateProfileRequest request,
			Authentication authentication) {
		assertCanActOnUser(id, authentication);
		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setFullName(request.getFullName());
		user.setAvatarUrl(request.getAvatarUrl());
		user.setBio(request.getBio());
		User updated = service.updateProfile(id, user);
		return UserMapper.toDTO(updated);
	}

	@PutMapping("/password/{id}")
	public ResponseEntity<Void> changePassword(@PathVariable int id, @Valid @RequestBody ChangePasswordRequest request,
			Authentication authentication) {
		assertCanActOnUser(id, authentication);
		service.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/search")
	public List<UserResponse> search(@RequestParam String username) {
		List<User> users = service.searchUsers(username);
		return users.stream().map(UserMapper::toDTO).collect(Collectors.toList());
	}

	@DeleteMapping("/deactivate/{id}")
	public ResponseEntity<Void> deactivate(@PathVariable int id, Authentication authentication) {
		assertCanActOnUser(id, authentication);
		service.deactivateAccount(id);
		return ResponseEntity.noContent().build();
	}

	private void assertCanActOnUser(int targetUserId, Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser currentUser)) {
			throw new AuthException("Authentication is required");
		}
		if (!currentUser.isAdmin() && currentUser.userId() != targetUserId) {
			throw new AccessDeniedException("Access denied");
		}
	}

	private String extractBearerToken(String authorizationHeader) {
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
			throw new AuthException("Bearer token is required");
		}
		return authorizationHeader.substring(7);
	}
}
