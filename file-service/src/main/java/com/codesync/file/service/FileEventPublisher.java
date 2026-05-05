package com.codesync.file.service;

import com.codesync.file.dto.FileUpdatedEvent;
import com.codesync.file.entity.CodeFile;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
public class FileEventPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final String exchangeName;
	private final String fileUpdatedRoutingKey;

	public FileEventPublisher(RabbitTemplate rabbitTemplate,
			@Value("${codesync.rabbit.exchange:codesync.events}") String exchangeName,
			@Value("${codesync.rabbit.routing-key.file-updated:file.updated}") String fileUpdatedRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchangeName = exchangeName;
		this.fileUpdatedRoutingKey = fileUpdatedRoutingKey;
	}

	public void publishFileUpdated(CodeFile file) {
		FileUpdatedEvent event = new FileUpdatedEvent();
		event.setEventId(UUID.randomUUID());
		event.setOccurredAt(Instant.now());
		event.setProjectId(file.getProjectId());
		event.setFileId(file.getFileId());
		event.setEditorId(file.getLastEditedBy());
		event.setPath(file.getPath());
		event.setContent(file.getContent());
		event.setMessage("Auto snapshot after updating " + file.getPath());
		event.setBranch("main");
		publishAfterCommit(event);
	}

	private void publishAfterCommit(FileUpdatedEvent event) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			rabbitTemplate.convertAndSend(exchangeName, fileUpdatedRoutingKey, event);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				rabbitTemplate.convertAndSend(exchangeName, fileUpdatedRoutingKey, event);
			}
		});
	}
}
