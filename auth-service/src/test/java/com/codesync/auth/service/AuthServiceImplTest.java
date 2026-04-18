package com.codesync.auth.service;

import com.codesync.auth.entity.User;
import com.codesync.auth.exception.AuthException;
import com.codesync.auth.repository.UserRepository;
import com.codesync.auth.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository repo;

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private AuthServiceImpl authService;

	private User user;

	@BeforeEach
	void setUp() {
		user = new User();
		user.setUserId(1);
		user.setUsername("testuser");
		user.setEmail("test@example.com");
		user.setPasswordHash("password");
		user.setActive(true);
	}

	// =========================
	// ✅ REGISTER
	// =========================
	@Test
	void register_Success() {
		when(repo.existsByEmail(user.getEmail())).thenReturn(false);
		when(repo.existsByUsername(user.getUsername())).thenReturn(false);
		when(repo.save(any(User.class))).thenReturn(user);

		User result = authService.register(user);

		assertNotNull(result);
		assertEquals("test@example.com", result.getEmail());
		verify(repo).save(any(User.class));
	}

	@Test
	void register_EmailExists() {
		when(repo.existsByEmail(user.getEmail())).thenReturn(true);

		assertThrows(AuthException.class, () -> authService.register(user));
	}

	@Test
	void register_UsernameExists() {
		when(repo.existsByEmail(user.getEmail())).thenReturn(false);
		when(repo.existsByUsername(user.getUsername())).thenReturn(true);

		assertThrows(AuthException.class, () -> authService.register(user));
	}

	
	@Test
	void login_Success() {
		// encoded password
		user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password"));

		when(repo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(jwtUtil.generateToken(user.getEmail())).thenReturn("token");

		String token = authService.login(user.getEmail(), "password");

		assertEquals("token", token);
	}

	@Test
	void login_UserNotFound() {
		when(repo.findByEmail(user.getEmail())).thenReturn(Optional.empty());

		assertThrows(AuthException.class, () -> authService.login(user.getEmail(), "password"));
	}

	@Test
	void login_InvalidPassword() {
		user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("correct"));

		when(repo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

		assertThrows(AuthException.class, () -> authService.login(user.getEmail(), "wrong"));
	}

	
	@Test
	void validateToken() {
		when(jwtUtil.validateToken("token")).thenReturn(true);

		assertTrue(authService.validateToken("token"));
	}

	@Test
	void refreshToken() {
		when(jwtUtil.extractEmail("oldToken")).thenReturn("test@example.com");
		when(jwtUtil.generateToken("test@example.com")).thenReturn("newToken");

		String token = authService.refreshToken("oldToken");

		assertEquals("newToken", token);
	}


	@Test
	void getUserByEmail() {
		when(repo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

		User result = authService.getUserByEmail(user.getEmail());

		assertEquals(user.getEmail(), result.getEmail());
	}

	@Test
	void getUserById() {
		when(repo.findByUserId(1)).thenReturn(user);

		User result = authService.getUserById(1);

		assertEquals(1, result.getUserId());
	}


	@Test
	void updateProfile() {
		when(repo.findByUserId(1)).thenReturn(user);
		when(repo.save(any(User.class))).thenReturn(user);

		User updated = new User();
		updated.setFullName("Deepak");
		updated.setAvatarUrl("img.png");
		updated.setBio("Developer");

		User result = authService.updateProfile(1, updated);

		assertEquals("Deepak", result.getFullName());
	}


	@Test
	void changePassword() {
		when(repo.findByUserId(1)).thenReturn(user);

		authService.changePassword(1, "newpass");

		assertNotEquals("newpass", user.getPasswordHash()); // encoded
		verify(repo).save(user);
	}


	@Test
	void searchUsers() {
		when(repo.findByUsernameContaining("test")).thenReturn(List.of(user));

		List<User> result = authService.searchUsers("test");

		assertFalse(result.isEmpty());
	}

	@Test
	void searchUsers_Blank() {
		assertThrows(AuthException.class, () -> authService.searchUsers(""));
	}


	@Test
	void deactivateAccount() {
		when(repo.findByUserId(1)).thenReturn(user);

		authService.deactivateAccount(1);

		assertFalse(user.isActive());
		verify(repo).save(user);
	}
}