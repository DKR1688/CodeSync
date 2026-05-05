package com.codesync.file.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.codesync.file.dto.FileTreeNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

class RedisConfigTest {

	private record TreeWrapper(List<FileTreeNode> nodes) {
	}

	@Test
	void redisSerializerRoundTripsNestedFileTreeWrapper() {
		GenericJackson2JsonRedisSerializer serializer = RedisConfig.redisValueSerializer();

		FileTreeNode root = new FileTreeNode(1L, "src", "src", true, null);
		root.getChildren().add(new FileTreeNode(2L, "App.java", "src/App.java", false, "java"));

		Object restored = serializer.deserialize(serializer.serialize(new TreeWrapper(List.of(root))));

		TreeWrapper restoredTree = assertInstanceOf(TreeWrapper.class, restored);
		assertEquals(1, restoredTree.nodes().size());
		assertEquals("src", restoredTree.nodes().get(0).getName());
		assertEquals(1, restoredTree.nodes().get(0).getChildren().size());
		assertEquals("App.java", restoredTree.nodes().get(0).getChildren().get(0).getName());
	}
}
