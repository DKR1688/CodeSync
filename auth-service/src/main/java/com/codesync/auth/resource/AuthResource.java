package com.codesync.auth.resource;

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
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthResource {

	private final AuthService service;

	public AuthResource(AuthService service) {
		this.service = service;
	}

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

	@GetMapping("/profile")
	public UserResponse getCurrentProfile(Authentication authentication) {
		return UserMapper.toDTO(service.getUserById(requireCurrentUserId(authentication)));
	}

	@PutMapping("/profile/{id}")
	public UserResponse updateProfile(@PathVariable int id, @Valid @RequestBody UpdateProfileRequest request,
			Authentication authentication) {
		assertCanActOnUser(id, authentication);
		return updateProfileInternal(id, request);
	}

	@PutMapping("/profile")
	public UserResponse updateCurrentProfile(@Valid @RequestBody UpdateProfileRequest request,
			Authentication authentication) {
		return updateProfileInternal(requireCurrentUserId(authentication), request);
	}

	@PutMapping("/password")
	public ResponseEntity<Void> changeCurrentPassword(@Valid @RequestBody ChangePasswordRequest request,
			Authentication authentication) {
		service.changePassword(requireCurrentUserId(authentication), request.getCurrentPassword(),
				request.getNewPassword());
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/deactivate")
	public ResponseEntity<Void> deactivateCurrentAccount(Authentication authentication) {
		service.deactivateAccount(requireCurrentUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/deactivate/{id}")
	public ResponseEntity<Void> deactivateById(@PathVariable int id, Authentication authentication) {
		assertCanActOnUser(id, authentication);
		service.deactivateAccount(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/password/{id}")
	public ResponseEntity<Void> changePassword(@PathVariable int id, @Valid @RequestBody ChangePasswordRequest request,
			Authentication authentication) {
		assertCanActOnUser(id, authentication);
		service.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/deactivate/{id}")
	public ResponseEntity<Void> deactivate(@PathVariable int id, Authentication authentication) {
		assertCanActOnUser(id, authentication);
		service.deactivateAccount(id);
		return ResponseEntity.noContent().build();
	}

	private UserResponse updateProfileInternal(int id, UpdateProfileRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setFullName(request.getFullName());
		user.setAvatarUrl(request.getAvatarUrl());
		user.setBio(request.getBio());
		User updated = service.updateProfile(id, user);
		return UserMapper.toDTO(updated);
	}

	@GetMapping("/search")
	public List<UserResponse> search(@RequestParam String username) {
		List<User> users = service.searchUsers(username);
		return users.stream().map(UserMapper::toDTO).collect(Collectors.toList());
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

	private int requireCurrentUserId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser currentUser)) {
			throw new AuthException("Authentication is required");
		}
		return currentUser.userId();
	}
}
