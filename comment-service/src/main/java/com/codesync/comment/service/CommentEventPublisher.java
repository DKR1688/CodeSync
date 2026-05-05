package com.codesync.comment.service;

import com.codesync.comment.dto.CommentEvent;
import com.codesync.comment.dto.NotificationRequest;
import com.codesync.comment.entity.Comment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Component
public class CommentEventPublisher {

	private final RabbitTemplate rabbitTemplate;
	private final String exchangeName;
	private final String notificationRoutingKey;
	private final String commentCreatedRoutingKey;
	private final String commentResolvedRoutingKey;

	public CommentEventPublisher(RabbitTemplate rabbitTemplate,
			@Value("${codesync.rabbit.exchange:codesync.events}") String exchangeName,
			@Value("${codesync.rabbit.routing-key.notification-send:notification.send}") String notificationRoutingKey,
			@Value("${codesync.rabbit.routing-key.comment-created:comment.created}") String commentCreatedRoutingKey,
			@Value("${codesync.rabbit.routing-key.comment-resolved:comment.resolved}") String commentResolvedRoutingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchangeName = exchangeName;
		this.notificationRoutingKey = notificationRoutingKey;
		this.commentCreatedRoutingKey = commentCreatedRoutingKey;
		this.commentResolvedRoutingKey = commentResolvedRoutingKey;
	}

	public void publishMentionNotification(NotificationRequest notificationRequest) {
		publishAfterCommit(notificationRoutingKey, notificationRequest);
	}

	public void publishCommentCreated(Comment comment) {
		publishAfterCommit(commentCreatedRoutingKey, toEvent(comment));
	}

	public void publishCommentResolved(Comment comment) {
		publishAfterCommit(commentResolvedRoutingKey, toEvent(comment));
	}

	private CommentEvent toEvent(Comment comment) {
		CommentEvent event = new CommentEvent();
		event.setEventId(UUID.randomUUID());
		event.setOccurredAt(Instant.now());
		event.setCommentId(comment.getCommentId());
		event.setProjectId(comment.getProjectId());
		event.setFileId(comment.getFileId());
		event.setAuthorId(comment.getAuthorId());
		event.setParentCommentId(comment.getParentCommentId());
		event.setSnapshotId(comment.getSnapshotId());
		event.setLineNumber(comment.getLineNumber());
		event.setColumnNumber(comment.getColumnNumber());
		event.setResolved(comment.isResolved());
		return event;
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
