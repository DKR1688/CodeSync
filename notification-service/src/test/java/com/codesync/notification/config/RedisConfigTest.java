package com.codesync.notification.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

class RedisConfigTest {

	@Test
	void redisSerializerRoundTripsCachedUnreadCount() {
		GenericJackson2JsonRedisSerializer serializer = RedisConfig.redisValueSerializer();

		Object restored = serializer.deserialize(serializer.serialize(5L));

		assertEquals(5L, ((Number) restored).longValue());
	}
}
