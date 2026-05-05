package com.codesync.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.codesync.auth.dto.UserResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

class RedisConfigTest {

	@Test
	void redisSerializerRoundTripsUserResponseWithJavaTimeField() {
		GenericJackson2JsonRedisSerializer serializer = RedisConfig.redisValueSerializer();

		UserResponse response = new UserResponse();
		response.setUserId(12);
		response.setUsername("cache-user");
		response.setEmail("cache@example.com");
		response.setFullName("Cache User");
		response.setRole("USER");
		response.setAvatarUrl("https://example.com/avatar.png");
		response.setProvider("LOCAL");
		response.setBio("Redis cache serialization smoke test");
		response.setActive(true);
		response.setCreatedAt(LocalDateTime.of(2026, 5, 5, 14, 55));

		Object restored = serializer.deserialize(serializer.serialize(response));

		UserResponse restoredResponse = assertInstanceOf(UserResponse.class, restored);
		assertEquals(response.getUserId(), restoredResponse.getUserId());
		assertEquals(response.getUsername(), restoredResponse.getUsername());
		assertEquals(response.getCreatedAt(), restoredResponse.getCreatedAt());
	}
}
