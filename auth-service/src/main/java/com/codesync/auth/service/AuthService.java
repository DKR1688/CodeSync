package com.codesync.auth.service;

import com.codesync.auth.entity.User;

import java.util.List;

public interface AuthService {

	User register(User user);

	User upsertOAuthUser(String email, String username, String fullName, String provider);

	String issueToken(User user);

	String login(String email, String password);

	void logout(String token);

	boolean validateToken(String token);

	String refreshToken(String token);

	User getUserByEmail(String email);

	User getUserById(int id);

	User updateProfile(int id, User user);

	void changePassword(int id, String currentPassword, String newPassword);

	List<User> searchUsers(String username);

	void deactivateAccount(int id);
}
