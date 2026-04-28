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
import io.swagger.v3.oas.annotations.Operation;
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
	@Operation(summary = "Register a new user", tags = { "1. Register" })
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPasswordHash(request.getPassword());
		user.setFullName(request.getFullName());

		User saved = service.register(user);
		return UserMapper.toDTO(saved);
	}

	@PostMapping("/admin/bootstrap")
	@Operation(summary = "Create the first admin user on a fresh system", tags = { "8. First-Time Setup" })
	public UserResponse bootstrapAdmin(@Valid @RequestBody RegisterRequest request) {
		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPasswordHash(request.getPassword());
		user.setFullName(request.getFullName());

		User saved = service.registerFirstAdmin(user);
		return UserMapper.toDTO(saved);
	}

	@PostMapping("/login")
	@Operation(summary = "Log in and get a JWT token", tags = { "2. Login" })
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		String token = service.login(request.getEmail(), request.getPassword());
		return new AuthResponse(token, "Login successful");
	}

	@PostMapping("/logout")
	@Operation(summary = "Log out the current user", tags = { "7. Session End" })
	public AuthResponse logout(@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		service.logout(extractBearerToken(authorizationHeader));
		return new AuthResponse(null, "Logout successful");
	}

	@PostMapping("/refresh")
	@Operation(summary = "Refresh the current JWT token", tags = { "6. Session Maintenance" })
	public AuthResponse refresh(@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
		String token = service.refreshToken(extractBearerToken(authorizationHeader));
		return new AuthResponse(token, "Token refreshed successfully");
	}

	@GetMapping("/profile/{id}")
	@Operation(summary = "Get a public profile by user id", tags = { "4. Public Lookup" })
	public UserResponse getProfile(@PathVariable int id) {
		User user = service.getUserById(id);
		return UserMapper.toDTO(user);
	}

	@GetMapping("/profile")
	@Operation(summary = "Get the currently logged-in user's profile", tags = { "3. Current User" })
	public UserResponse getCurrentProfile(Authentication authentication) {
		return UserMapper.toDTO(service.getUserById(requireCurrentUserId(authentication)));
	}

	@PutMapping("/profile/{id}")
	@Operation(summary = "Update a profile by user id as self or admin", tags = { "5. User Maintenance" })
	public UserResponse updateProfile(@PathVariable int id, @Valid @RequestBody UpdateProfileRequest request,
			Authentication authentication) {
		assertCanActOnUser(id, authentication);
		return updateProfileInternal(id, request);
	}

	@PutMapping("/profile")
	@Operation(summary = "Update the currently logged-in user's profile", tags = { "5. User Maintenance" })
	public UserResponse updateCurrentProfile(@Valid @RequestBody UpdateProfileRequest request,
			Authentication authentication) {
		return updateProfileInternal(requireCurrentUserId(authentication), request);
	}

	@PutMapping("/password")
	@Operation(summary = "Change the current user's password", tags = { "5. User Maintenance" })
	public ResponseEntity<Void> changeCurrentPassword(@Valid @RequestBody ChangePasswordRequest request,
			Authentication authentication) {
		service.changePassword(requireCurrentUserId(authentication), request.getCurrentPassword(),
				request.getNewPassword());
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/deactivate")
	@Operation(summary = "Deactivate the current user's account", tags = { "5. User Maintenance" })
	public ResponseEntity<Void> deactivateCurrentAccount(Authentication authentication) {
		service.deactivateAccount(requireCurrentUserId(authentication));
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/deactivate/{id}")
	@Operation(summary = "Deactivate a user by id as self or admin", tags = { "5. User Maintenance" })
	public ResponseEntity<Void> deactivateById(@PathVariable int id, Authentication authentication) {
		assertCanActOnUser(id, authentication);
		service.deactivateAccount(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/password/{id}")
	@Operation(summary = "Change a user's password by id as self or admin", tags = { "5. User Maintenance" })
	public ResponseEntity<Void> changePassword(@PathVariable int id, @Valid @RequestBody ChangePasswordRequest request,
			Authentication authentication) {
		assertCanActOnUser(id, authentication);
		service.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/deactivate/{id}")
	@Operation(summary = "Deactivate a user by id using the delete route", tags = { "5. User Maintenance" })
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
	@Operation(summary = "Search users by username", tags = { "4. Public Lookup" })
	public List<UserResponse> search(@RequestParam String username) {
		List<User> users = service.searchUsers(username);
		return users.stream().map(UserMapper::toDTO).collect(Collectors.toList());
	}

	@GetMapping("/users")
	@Operation(summary = "List all users as admin", tags = { "9. Admin Only" })
	public List<UserResponse> getAllUsers(Authentication authentication) {
		assertAdmin(authentication);
		return service.getAllUsers().stream().map(UserMapper::toDTO).collect(Collectors.toList());
	}

	@PutMapping("/reactivate/{id}")
	@Operation(summary = "Reactivate a user by id as admin", tags = { "9. Admin Only" })
	public ResponseEntity<Void> reactivate(@PathVariable int id, Authentication authentication) {
		assertAdmin(authentication);
		service.reactivateAccount(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/users/{id}")
	@Operation(summary = "Delete a user permanently as admin", tags = { "9. Admin Only" })
	public ResponseEntity<Void> deleteUser(@PathVariable int id, Authentication authentication) {
		assertAdmin(authentication);
		service.deleteAccount(id);
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

	private void assertAdmin(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser currentUser)) {
			throw new AuthException("Authentication is required");
		}
		if (!currentUser.isAdmin()) {
			throw new AccessDeniedException("Admin access is required");
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
