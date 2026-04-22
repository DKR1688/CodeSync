package com.codesync.auth.repository;

import com.codesync.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	User findByUserId(int userId);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByEmailAndUserIdNot(String email, int userId);

	boolean existsByUsernameAndUserIdNot(String username, int userId);

	List<User> findAllByRole(String role);

	boolean existsByRoleIgnoreCase(String role);

	List<User> findByUsernameContainingIgnoreCase(String username);

	void deleteByUserId(int userId);
}
