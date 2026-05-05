package com.codesync.project.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.codesync.project.dto.ProjectDTO;
import com.codesync.project.enums.Visibility;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

class RedisConfigTest {

	@Test
	void redisSerializerRoundTripsProjectDtoWithJavaTimeFields() {
		GenericJackson2JsonRedisSerializer serializer = RedisConfig.redisValueSerializer();

		ProjectDTO project = new ProjectDTO();
		project.setProjectId(3L);
		project.setOwnerId(7L);
		project.setName("Cache Smoke Test");
		project.setDescription("Project cached through Redis");
		project.setLanguage("Java");
		project.setVisibility(Visibility.PRIVATE);
		project.setTemplateId(11L);
		project.setArchived(false);
		project.setCreatedAt(LocalDateTime.of(2026, 5, 5, 14, 30));
		project.setUpdatedAt(LocalDateTime.of(2026, 5, 5, 14, 45));
		project.setStarCount(2);
		project.setForkCount(1);
		project.setMemberUserIds(new LinkedHashSet<>(java.util.Set.of(7L, 8L)));

		Object restored = serializer.deserialize(serializer.serialize(project));

		ProjectDTO restoredProject = assertInstanceOf(ProjectDTO.class, restored);
		assertEquals(project.getProjectId(), restoredProject.getProjectId());
		assertEquals(project.getOwnerId(), restoredProject.getOwnerId());
		assertEquals(project.getName(), restoredProject.getName());
		assertEquals(project.getCreatedAt(), restoredProject.getCreatedAt());
		assertEquals(project.getUpdatedAt(), restoredProject.getUpdatedAt());
		assertEquals(project.getMemberUserIds(), restoredProject.getMemberUserIds());
	}
}
