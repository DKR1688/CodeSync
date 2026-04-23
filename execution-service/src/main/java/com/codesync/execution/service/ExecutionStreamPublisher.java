package com.codesync.execution.service;

import com.codesync.execution.enums.ExecutionStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class ExecutionStreamPublisher {

	private final SimpMessagingTemplate messagingTemplate;

	public ExecutionStreamPublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	public void publishStdout(UUID jobId, String chunk) {
		Object payload = Map.of("jobId", jobId, "stream", "stdout", "chunk", chunk);
		messagingTemplate.convertAndSend("/topic/executions/" + jobId + "/stdout",
				payload);
	}

	public void publishStatus(UUID jobId, ExecutionStatus status) {
		Object payload = Map.of("jobId", jobId, "status", status);
		messagingTemplate.convertAndSend("/topic/executions/" + jobId + "/status",
				payload);
	}
}
