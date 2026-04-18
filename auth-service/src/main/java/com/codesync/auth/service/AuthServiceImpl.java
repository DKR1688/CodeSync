package com.codesync.auth.service;

import com.codesync.auth.entity.User;
import com.codesync.auth.exception.AuthException;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.security.JwtUtil;
import com.codesync.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

	@Autowired
	private UserRepository repo;

	@Autowired
	private JwtUtil jwtUtil;

	private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Override
	public User register(User user) {
		if (repo.existsByEmail(user.getEmail()))
			throw new AuthException("Email already exists");

		if (repo.existsByUsername(user.getUsername()))
			throw new AuthException("Username already exists");

		user.setPasswordHash(encoder.encode(user.getPasswordHash()));
		user.setRole(user.getRole() != null ? user.getRole() : "DEVELOPER");
		user.setProvider("LOCAL");
		user.setActive(true);
		user.setCreatedAt(LocalDateTime.now());

		return repo.save(user);
	}

	@Override
	public String login(String email, String password) {
		logger.debug("Login attempt for email: {}", email);
		User user = repo.findByEmail(email).orElseThrow(() -> new AuthException("User not found"));

		if (!encoder.matches(password, user.getPasswordHash())) {
			logger.warn("Invalid password for email: {}", email);
			throw new AuthException("Invalid password");
		}

		try {
			String token = jwtUtil.generateToken(email);
			logger.debug("Generated JWT token successfully for email: {}", email);
			return token;
		} catch (Exception ex) {
			logger.error("Failed to generate JWT token for email {}", email, ex);
			throw new RuntimeException("Failed to generate authentication token", ex);
		}
	}

	@Override
	public void logout(String token) {
		// Stateless JWT → usually handled client-side
	}

	@Override
	public boolean validateToken(String token) {
		return jwtUtil.validateToken(token);
	}

	@Override
	public String refreshToken(String token) {
		String email = jwtUtil.extractEmail(token);
		return jwtUtil.generateToken(email);
	}

	@Override
	public User getUserByEmail(String email) {
		return repo.findByEmail(email).orElse(null);
	}

	@Override
	public User getUserById(int id) {
		return findUserById(id);
	}

	@Override
	public User updateProfile(int id, User user) {
		User existing = findUserById(id);
		existing.setFullName(user.getFullName());
		existing.setAvatarUrl(user.getAvatarUrl());
		existing.setBio(user.getBio());
		return repo.save(existing);
	}

	@Override
	public void changePassword(int id, String password) {
		User user = findUserById(id);
		user.setPasswordHash(encoder.encode(password));
		repo.save(user);
	}

	@Override
	public List<User> searchUsers(String username) {
		if (username == null || username.isBlank()) {
			throw new AuthException("Username search query cannot be blank");
		}
		return repo.findByUsernameContaining(username);
	}

	@Override
	public void deactivateAccount(int id) {
		User user = findUserById(id);
		user.setActive(false);
		repo.save(user);
	}

	private User findUserById(int id) {
		User user = repo.findByUserId(id);
		if (user == null) {
			throw new AuthException("User not found");
		}
		return user;
	}
}