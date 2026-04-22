package com.codesync.auth.service;

import com.codesync.auth.entity.User;
import com.codesync.auth.exception.AuthException;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

	private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

	@Autowired
	private UserRepository repo;

	@Autowired
	private JwtUtil jwtUtil;

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Override
	public User register(User user) {
		validateNewUserIdentity(user.getEmail(), user.getUsername());
		if (!StringUtils.hasText(user.getPasswordHash())) {
			throw new AuthException("Password is required");
		}

		user.setEmail(normalizeEmail(user.getEmail()));
		user.setUsername(normalizeUsername(user.getUsername()));
		user.setPasswordHash(encoder.encode(user.getPasswordHash()));
		user.setRole(defaultRole(user.getRole()));
		user.setProvider(defaultProvider(user.getProvider()));
		user.setActive(true);
		user.setCreatedAt(LocalDateTime.now());
		return repo.save(user);
	}

	@Override
	public User registerFirstAdmin(User user) {
		if (repo.existsByRoleIgnoreCase("ADMIN")) {
			throw new AuthException("Admin user already exists");
		}
		user.setRole("ADMIN");
		return register(user);
	}

	@Override
	public User upsertOAuthUser(String email, String username, String fullName, String provider) {
		String normalizedEmail = normalizeEmail(email);
		String normalizedProvider = defaultProvider(provider);
		User existingUser = repo.findByEmail(normalizedEmail).orElse(null);

		if (existingUser != null) {
			existingUser.setProvider(normalizedProvider);
			existingUser.setActive(true);
			if (StringUtils.hasText(fullName)) {
				existingUser.setFullName(fullName.trim());
			}
			if (!StringUtils.hasText(existingUser.getUsername())) {
				existingUser.setUsername(generateAvailableUsername(username, normalizedEmail));
			}
			return repo.save(existingUser);
		}

		User user = new User();
		user.setEmail(normalizedEmail);
		user.setUsername(generateAvailableUsername(username, normalizedEmail));
		user.setFullName(StringUtils.hasText(fullName) ? fullName.trim() : user.getUsername());
		user.setRole("DEVELOPER");
		user.setProvider(normalizedProvider);
		user.setActive(true);
		user.setCreatedAt(LocalDateTime.now());
		return repo.save(user);
	}

	@Override
	public String issueToken(User user) {
		return jwtUtil.generateToken(user);
	}

	@Override
	public String login(String email, String password) {
		String normalizedEmail = normalizeEmail(email);
		logger.debug("Login attempt for email: {}", normalizedEmail);
		User user = repo.findByEmail(normalizedEmail).orElseThrow(() -> new AuthException("User not found"));
		requireActiveUser(user);

		if (!StringUtils.hasText(user.getPasswordHash())) {
			throw new AuthException("Use your OAuth provider to log in");
		}
		if (!encoder.matches(password, user.getPasswordHash())) {
			logger.warn("Invalid password for email: {}", normalizedEmail);
			throw new AuthException("Invalid password");
		}

		try {
			String token = issueToken(user);
			logger.debug("Generated JWT token successfully for email: {}", normalizedEmail);
			return token;
		} catch (Exception ex) {
			logger.error("Failed to generate JWT token for email {}", normalizedEmail, ex);
			throw new RuntimeException("Failed to generate authentication token", ex);
		}
	}

	@Override
	public void logout(String token) {
		if (!jwtUtil.validateToken(token)) {
			throw new AuthException("Invalid token");
		}
		jwtUtil.revokeToken(token);
	}

	@Override
	public boolean validateToken(String token) {
		return jwtUtil.validateToken(token);
	}

	@Override
	public String refreshToken(String token) {
		if (!jwtUtil.validateToken(token)) {
			throw new AuthException("Invalid token");
		}
		User user = getUserByEmail(jwtUtil.extractEmail(token));
		if (user == null) {
			throw new AuthException("User not found");
		}
		requireActiveUser(user);
		return issueToken(user);
	}

	@Override
	public User getUserByEmail(String email) {
		return repo.findByEmail(normalizeEmail(email)).orElse(null);
	}

	@Override
	public User getUserById(int id) {
		return findUserById(id);
	}

	@Override
	public User updateProfile(int id, User user) {
		User existing = findUserById(id);

		if (user.getUsername() != null) {
			String normalizedUsername = normalizeUsername(user.getUsername());
			if (repo.existsByUsernameAndUserIdNot(normalizedUsername, id)) {
				throw new AuthException("Username already exists");
			}
			existing.setUsername(normalizedUsername);
		}

		if (user.getEmail() != null) {
			String normalizedEmail = normalizeEmail(user.getEmail());
			if (repo.existsByEmailAndUserIdNot(normalizedEmail, id)) {
				throw new AuthException("Email already exists");
			}
			existing.setEmail(normalizedEmail);
		}

		if (user.getFullName() != null) {
			existing.setFullName(requireText(user.getFullName(), "Full name"));
		}
		if (user.getAvatarUrl() != null) {
			existing.setAvatarUrl(blankToNull(user.getAvatarUrl()));
		}
		if (user.getBio() != null) {
			existing.setBio(blankToNull(user.getBio()));
		}

		return repo.save(existing);
	}

	@Override
	public void changePassword(int id, String currentPassword, String newPassword) {
		User user = findUserById(id);
		requireActiveUser(user);
		if (!StringUtils.hasText(user.getPasswordHash())) {
			throw new AuthException("Password login is not enabled for this account");
		}
		if (!encoder.matches(currentPassword, user.getPasswordHash())) {
			throw new AuthException("Current password is incorrect");
		}
		user.setPasswordHash(encoder.encode(newPassword));
		repo.save(user);
	}

	@Override
	public List<User> searchUsers(String username) {
		if (username == null || username.isBlank()) {
			throw new AuthException("Username search query cannot be blank");
		}
		return repo.findByUsernameContainingIgnoreCase(username.trim());
	}

	@Override
	public void deactivateAccount(int id) {
		User user = findUserById(id);
		user.setActive(false);
		repo.save(user);
	}

	private void validateNewUserIdentity(String email, String username) {
		String normalizedEmail = normalizeEmail(email);
		String normalizedUsername = normalizeUsername(username);
		if (repo.existsByEmail(normalizedEmail)) {
			throw new AuthException("Email already exists");
		}
		if (repo.existsByUsername(normalizedUsername)) {
			throw new AuthException("Username already exists");
		}
	}

	private void requireActiveUser(User user) {
		if (!user.isActive()) {
			throw new AuthException("Account is deactivated");
		}
	}

	private String generateAvailableUsername(String preferredUsername, String email) {
		String base = StringUtils.hasText(preferredUsername)
				? normalizeUsername(preferredUsername)
				: normalizeUsername(email.substring(0, email.indexOf('@')));
		String candidate = base;
		int suffix = 1;
		while (repo.existsByUsername(candidate)) {
			candidate = base + suffix++;
		}
		return candidate;
	}

	private String normalizeEmail(String email) {
		String value = requireText(email, "Email");
		return value.toLowerCase(Locale.ROOT);
	}

	private String normalizeUsername(String username) {
		return requireText(username, "Username");
	}

	private String requireText(String value, String fieldName) {
		if (!StringUtils.hasText(value)) {
			throw new AuthException(fieldName + " is required");
		}
		return value.trim();
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String defaultRole(String role) {
		return StringUtils.hasText(role) ? role.trim().toUpperCase(Locale.ROOT) : "DEVELOPER";
	}

	private String defaultProvider(String provider) {
		return StringUtils.hasText(provider) ? provider.trim().toUpperCase(Locale.ROOT) : "LOCAL";
	}

	private User findUserById(int id) {
		User user = repo.findByUserId(id);
		if (user == null) {
			throw new AuthException("User not found");
		}
		return user;
	}
}
