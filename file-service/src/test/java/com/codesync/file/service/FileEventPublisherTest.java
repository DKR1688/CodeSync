package com.codesync.file.service;

import com.codesync.file.dto.FileUpdatedEvent;
import com.codesync.file.entity.CodeFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileEventPublisherTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Test
	void publishFileUpdatedBuildsSnapshotEvent() {
		FileEventPublisher publisher = new FileEventPublisher(rabbitTemplate, "codesync.events", "file.updated");

		CodeFile file = new CodeFile();
		file.setFileId(3L);
		file.setProjectId(9L);
		file.setLastEditedBy(12L);
		file.setPath("src/Main.java");
		file.setContent("class Main {}");

		publisher.publishFileUpdated(file);

		ArgumentCaptor<FileUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(FileUpdatedEvent.class);
		verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("codesync.events"),
				org.mockito.ArgumentMatchers.eq("file.updated"), eventCaptor.capture());

		FileUpdatedEvent event = eventCaptor.getValue();
		assertThat(event.getEventId()).isNotNull();
		assertThat(event.getOccurredAt()).isNotNull();
		assertThat(event.getProjectId()).isEqualTo(9L);
		assertThat(event.getFileId()).isEqualTo(3L);
		assertThat(event.getEditorId()).isEqualTo(12L);
		assertThat(event.getPath()).isEqualTo("src/Main.java");
		assertThat(event.getContent()).isEqualTo("class Main {}");
		assertThat(event.getMessage()).isEqualTo("Auto snapshot after updating src/Main.java");
		assertThat(event.getBranch()).isEqualTo("main");
	}
}
