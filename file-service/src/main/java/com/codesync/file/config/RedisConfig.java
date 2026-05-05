package com.codesync.file.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisConfig {

	static GenericJackson2JsonRedisSerializer redisValueSerializer() {
		return new GenericJackson2JsonRedisSerializer()
				.configure(objectMapper -> objectMapper.findAndRegisterModules());
	}

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration defaultConfiguration = baseConfiguration(Duration.ofMinutes(5));
		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(defaultConfiguration)
				.withInitialCacheConfigurations(Map.of(
						"files.tree", baseConfiguration(Duration.ofMinutes(3))))
				.build();
	}

	private RedisCacheConfiguration baseConfiguration(Duration ttl) {
		return RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(ttl)
				.disableCachingNullValues()
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(redisValueSerializer()));
	}
}
