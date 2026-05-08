package com.codesync.file.service;

import com.codesync.file.client.VersionServiceClient;
import com.codesync.file.dto.CreateSnapshotRequest;
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
	private final VersionServiceClient versionServiceClient;
	private final String exchangeName;
	private final String fileUpdatedRoutingKey;
	@Value("${codesync.rabbit.enabled:false}")
	private boolean rabbitEnabled;

	public FileEventPublisher(RabbitTemplate rabbitTemplate,
			VersionServiceClient versionServiceClient,
			@Value("${codesync.rabbit.exchange:codesync.events}") String exchangeName,
			@Value("${codesync.rabbit.routing-key.file-updated:file.updated}") String fileUpdatedRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.versionServiceClient = versionServiceClient;
		this.exchangeName = exchangeName;
		this.fileUpdatedRoutingKey = fileUpdatedRoutingKey;
	}

	public void publishFileUpdated(CodeFile file, String authorizationHeader) {
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
		publishAfterCommit(event, authorizationHeader);
	}

	private void publishAfterCommit(FileUpdatedEvent event, String authorizationHeader) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			dispatch(event, authorizationHeader);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				dispatch(event, authorizationHeader);
			}
		});
	}

	private void dispatch(FileUpdatedEvent event, String authorizationHeader) {
		if (rabbitEnabled) {
			rabbitTemplate.convertAndSend(exchangeName, fileUpdatedRoutingKey, event);
			return;
		}

		versionServiceClient.createSnapshot(toCreateSnapshotRequest(event), authorizationHeader);
	}

	private CreateSnapshotRequest toCreateSnapshotRequest(FileUpdatedEvent event) {
		CreateSnapshotRequest request = new CreateSnapshotRequest();
		request.setProjectId(event.getProjectId());
		request.setFileId(event.getFileId());
		request.setContent(event.getContent());
		request.setMessage(event.getMessage());
		request.setBranch(event.getBranch());
		return request;
	}
}
