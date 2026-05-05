package com.codesync.project.service;

import com.codesync.project.dto.NotificationCommand;
import com.codesync.project.dto.ProjectDTO;
import com.codesync.project.dto.ProjectEvent;
import com.codesync.project.dto.ProjectMemberEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
public class ProjectEventPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final String exchangeName;
	private final String notificationRoutingKey;
	private final String projectCreatedRoutingKey;
	private final String projectMemberAddedRoutingKey;

	public ProjectEventPublisher(RabbitTemplate rabbitTemplate,
			@Value("${codesync.rabbit.exchange}") String exchangeName,
			@Value("${codesync.rabbit.routing-key.notification-send}") String notificationRoutingKey,
			@Value("${codesync.rabbit.routing-key.project-created}") String projectCreatedRoutingKey,
			@Value("${codesync.rabbit.routing-key.project-member-added}") String projectMemberAddedRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchangeName = exchangeName;
		this.notificationRoutingKey = notificationRoutingKey;
		this.projectCreatedRoutingKey = projectCreatedRoutingKey;
		this.projectMemberAddedRoutingKey = projectMemberAddedRoutingKey;
	}

	public void publishProjectCreated(ProjectDTO project) {
		ProjectEvent event = new ProjectEvent();
		event.setEventId(UUID.randomUUID());
		event.setOccurredAt(Instant.now());
		event.setProjectId(project.getProjectId());
		event.setOwnerId(project.getOwnerId());
		event.setName(project.getName());
		event.setLanguage(project.getLanguage());
		event.setVisibility(project.getVisibility() != null ? project.getVisibility().name() : null);
		publishAfterCommit(projectCreatedRoutingKey, event);
	}

	public void publishMemberAdded(ProjectDTO project, Long memberUserId, Long actorId) {
		ProjectMemberEvent event = new ProjectMemberEvent();
		event.setEventId(UUID.randomUUID());
		event.setOccurredAt(Instant.now());
		event.setProjectId(project.getProjectId());
		event.setMemberUserId(memberUserId);
		event.setActorId(actorId);
		event.setProjectName(project.getName());
		publishAfterCommit(projectMemberAddedRoutingKey, event);

		NotificationCommand notification = new NotificationCommand();
		notification.setRecipientId(memberUserId);
		notification.setActorId(actorId);
		notification.setType("SESSION_INVITE");
		notification.setTitle("You were added to a project");
		notification.setMessage("You were added to project '" + project.getName() + "'");
		notification.setRelatedId(String.valueOf(project.getProjectId()));
		notification.setRelatedType("PROJECT");
		notification.setDeepLinkUrl("/projects/" + project.getProjectId());
		publishAfterCommit(notificationRoutingKey, notification);
	}

	private void publishAfterCommit(String routingKey, Object payload) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				rabbitTemplate.convertAndSend(exchangeName, routingKey, payload);
			}
		});
	}
}
