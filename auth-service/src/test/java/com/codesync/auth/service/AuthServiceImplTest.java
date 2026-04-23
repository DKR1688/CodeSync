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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

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
		user.setRole("DEVELOPER");
		user.setProvider("LOCAL");
		user.setActive(true);
		user.setFullName("Test User");
	}

	@Test
	void registerSuccessEncodesPasswordAndDefaultsProvider() {
		when(repo.existsByEmail("test@example.com")).thenReturn(false);
		when(repo.existsByUsername("testuser")).thenReturn(false);
		when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = authService.register(user);

		assertNotNull(result);
		assertNotEquals("password", result.getPasswordHash());
		assertEquals("LOCAL", result.getProvider());
		verify(repo).save(any(User.class));
	}

	@Test
	void registerRejectsDuplicateEmail() {
		when(repo.existsByEmail("test@example.com")).thenReturn(true);

		assertThrows(AuthException.class, () -> authService.register(user));
		verify(repo, never()).save(any(User.class));
	}

	@Test
	void loginSuccessReturnsJwt() {
		user.setPasswordHash(encoder.encode("password"));
		when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		when(jwtUtil.generateToken(user)).thenReturn("token");

		String token = authService.login("test@example.com", "password");

		assertEquals("token", token);
	}

	@Test
	void loginRejectsInactiveUsers() {
		user.setActive(false);
		when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(user));

		assertThrows(AuthException.class, () -> authService.login("test@example.com", "password"));
	}

	@Test
	void refreshTokenReissuesForActiveUser() {
		when(jwtUtil.validateToken("oldToken")).thenReturn(true);
		when(jwtUtil.extractEmail("oldToken")).thenReturn("test@example.com");
		when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		when(jwtUtil.generateToken(user)).thenReturn("newToken");

		String token = authService.refreshToken("oldToken");

		assertEquals("newToken", token);
	}

	@Test
	void logoutRevokesValidToken() {
		when(jwtUtil.validateToken("validToken")).thenReturn(true);
		doNothing().when(jwtUtil).revokeToken("validToken");

		authService.logout("validToken");

		verify(jwtUtil).revokeToken("validToken");
	}

	@Test
	void upsertOAuthUserCreatesAvailableUsername() {
		when(repo.findByEmail("oauth@example.com")).thenReturn(Optional.empty());
		when(repo.existsByUsername("oauth")).thenReturn(true);
		when(repo.existsByUsername("oauth1")).thenReturn(false);
		when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User created = authService.upsertOAuthUser("oauth@example.com", "oauth", "OAuth User", "google");

		assertEquals("oauth1", created.getUsername());
		assertEquals("GOOGLE", created.getProvider());
	}

	@Test
	void updateProfileChangesUsernameEmailAndBio() {
		User update = new User();
		update.setUsername("renamed");
		update.setEmail("renamed@example.com");
		update.setFullName("Renamed User");
		update.setBio("Builder");

		when(repo.findByUserId(1)).thenReturn(user);
		when(repo.existsByUsernameAndUserIdNot("renamed", 1)).thenReturn(false);
		when(repo.existsByEmailAndUserIdNot("renamed@example.com", 1)).thenReturn(false);
		when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User result = authService.updateProfile(1, update);

		assertEquals("renamed", result.getUsername());
		assertEquals("renamed@example.com", result.getEmail());
		assertEquals("Builder", result.getBio());
	}

	@Test
	void changePasswordRequiresCurrentPasswordMatch() {
		user.setPasswordHash(encoder.encode("oldPass"));
		when(repo.findByUserId(1)).thenReturn(user);

		authService.changePassword(1, "oldPass", "newPass123");

		assertTrue(encoder.matches("newPass123", user.getPasswordHash()));
		verify(repo).save(user);
	}

	@Test
	void searchUsersRejectsBlankQuery() {
		assertThrows(AuthException.class, () -> authService.searchUsers(" "));
	}

	@Test
	void searchUsersUsesCaseInsensitiveRepositoryQuery() {
		when(repo.findByUsernameContainingIgnoreCase("test")).thenReturn(List.of(user));

		List<User> result = authService.searchUsers("test");

		assertEquals(1, result.size());
	}

	@Test
	void deactivateAccountMarksUserInactive() {
		when(repo.findByUserId(1)).thenReturn(user);

		authService.deactivateAccount(1);

		assertFalse(user.isActive());
		verify(repo).save(user);
	}
}
