package com.codesync.version.service;

import com.codesync.version.dto.FileUpdatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class VersionSnapshotListener {

	private final VersionServiceImpl versionService;

	public VersionSnapshotListener(VersionServiceImpl versionService) {
		this.versionService = versionService;
	}

	@RabbitListener(queues = "${codesync.rabbit.queue.version-snapshots:version.snapshot.events}")
	public void handleFileUpdated(FileUpdatedEvent event) {
		versionService.createSnapshotFromFileUpdate(event);
	}
}
