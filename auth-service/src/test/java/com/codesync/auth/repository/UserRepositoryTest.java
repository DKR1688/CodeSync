package com.codesync.auth.repository;

import com.codesync.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_ShouldReturnUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        entityManager.persist(user);
        entityManager.flush();

        User found = userRepository.findByEmail("test@example.com").orElse(null);

        assertNotNull(found);
        assertEquals("test@example.com", found.getEmail());
    }

    @Test
    void findByUsername_ShouldReturnUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        entityManager.persist(user);
        entityManager.flush();

        User found = userRepository.findByUsername("testuser").orElse(null);

        assertNotNull(found);
        assertEquals("testuser", found.getUsername());
    }

    @Test
    void findByUserId_ShouldReturnUser() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("testuser");
        entityManager.persist(user);
        entityManager.flush();

        User found = userRepository.findByUserId(1);

        assertNotNull(found);
        assertEquals(1, found.getUserId());
    }

    @Test
    void existsByEmail_ShouldReturnTrue() {
        User user = new User();
        user.setEmail("test@example.com");
        entityManager.persist(user);
        entityManager.flush();

        boolean exists = userRepository.existsByEmail("test@example.com");

        assertTrue(exists);
    }

    @Test
    void findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase_ShouldReturnUsers() {
        User user1 = new User();
        user1.setUsername("testuser");
        user1.setEmail("test@example.com");

        User user2 = new User();
        user2.setUsername("otheruser");
        user2.setEmail("other@test.com");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        List<User> users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase("test", "test");

        assertEquals(2, users.size());
    }
}