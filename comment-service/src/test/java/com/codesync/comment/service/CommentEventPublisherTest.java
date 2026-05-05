package com.codesync.comment.service;

import com.codesync.comment.dto.CommentEvent;
import com.codesync.comment.dto.NotificationRequest;
import com.codesync.comment.entity.Comment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CommentEventPublisherTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@AfterEach
	void clearSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void publishCommentCreatedMapsCommentFieldsIntoEvent() {
		CommentEventPublisher publisher = new CommentEventPublisher(
				rabbitTemplate,
				"codesync.events",
				"notification.send",
				"comment.created",
				"comment.resolved");

		Comment comment = new Comment();
		comment.setCommentId(10L);
		comment.setProjectId(20L);
		comment.setFileId(30L);
		comment.setAuthorId(40L);
		comment.setParentCommentId(50L);
		comment.setSnapshotId(60L);
		comment.setLineNumber(12);
		comment.setColumnNumber(4);
		comment.setResolved(true);

		publisher.publishCommentCreated(comment);

		ArgumentCaptor<CommentEvent> eventCaptor = ArgumentCaptor.forClass(CommentEvent.class);
		verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("codesync.events"),
				org.mockito.ArgumentMatchers.eq("comment.created"), eventCaptor.capture());

		CommentEvent event = eventCaptor.getValue();
		assertThat(event.getEventId()).isNotNull();
		assertThat(event.getOccurredAt()).isNotNull();
		assertThat(event.getCommentId()).isEqualTo(10L);
		assertThat(event.getProjectId()).isEqualTo(20L);
		assertThat(event.getFileId()).isEqualTo(30L);
		assertThat(event.getAuthorId()).isEqualTo(40L);
		assertThat(event.getParentCommentId()).isEqualTo(50L);
		assertThat(event.getSnapshotId()).isEqualTo(60L);
		assertThat(event.getLineNumber()).isEqualTo(12);
		assertThat(event.getColumnNumber()).isEqualTo(4);
		assertThat(event.isResolved()).isTrue();
	}

	@Test
	void publishMentionNotificationWaitsForTransactionCommitWhenSynchronizationIsActive() {
		CommentEventPublisher publisher = new CommentEventPublisher(
				rabbitTemplate,
				"codesync.events",
				"notification.send",
				"comment.created",
				"comment.resolved");
		NotificationRequest request = new NotificationRequest();
		request.setRecipientId(77L);

		TransactionSynchronizationManager.initSynchronization();

		publisher.publishMentionNotification(request);

		verifyNoInteractions(rabbitTemplate);
		assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

		for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
			synchronization.afterCommit();
		}

		verify(rabbitTemplate).convertAndSend("codesync.events", "notification.send", request);
	}
}
