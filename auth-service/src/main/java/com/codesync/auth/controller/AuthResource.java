package com.codesync.auth.controller;

import com.codesync.auth.dto.AuthResponse;
import com.codesync.auth.dto.LoginRequest;
import com.codesync.auth.dto.RegisterRequest;
import com.codesync.auth.dto.UpdateProfileRequest;
import com.codesync.auth.dto.UserResponse;
import com.codesync.auth.entity.User;
import com.codesync.auth.mapper.UserMapper;
import com.codesync.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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

	@GetMapping("/profile/{id}")
	public UserResponse getProfile(@PathVariable int id) {
		User user = service.getUserById(id);
		return UserMapper.toDTO(user);
	}

	@PutMapping("/profile/{id}")
	public UserResponse updateProfile(@PathVariable int id, @Valid @RequestBody UpdateProfileRequest request) {
		User user = new User();
		user.setFullName(request.getFullName());
		user.setAvatarUrl(request.getAvatarUrl());
		user.setBio(request.getBio());
		User updated = service.updateProfile(id, user);
		return UserMapper.toDTO(updated);
	}

	@PostMapping("/password")
	public void changePassword(@RequestParam int id, @RequestParam String password) {
		service.changePassword(id, password);
	}

	@GetMapping("/search")
	public List<UserResponse> search(@RequestParam String username) {
		List<User> users = service.searchUsers(username);
		return users.stream().map(UserMapper::toDTO).collect(Collectors.toList());
	}

	@DeleteMapping("/deactivate/{id}")
	public void deactivate(@PathVariable int id) {
		service.deactivateAccount(id);
	}
}