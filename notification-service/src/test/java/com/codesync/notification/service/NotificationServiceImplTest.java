package com.codesync.notification.service;

import com.codesync.notification.dto.BulkNotificationRequest;
import com.codesync.notification.dto.SendNotificationRequest;
import com.codesync.notification.entity.Notification;
import com.codesync.notification.enums.NotificationType;
import com.codesync.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceImplTest {

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationRepository repository;

	@MockitoBean
	private NotificationPublisher publisher;

	@BeforeEach
	void cleanRepository() {
		repository.deleteAll();
	}

	@Test
	void sendPersistsUnreadNotificationAndPublishesBadge() {
		Notification notification = notificationService.send(request(20L, NotificationType.MENTION));

		assertThat(notification.getNotificationId()).isNotNull();
		assertThat(notification.getRecipientId()).isEqualTo(20L);
		assertThat(notification.getActorId()).isEqualTo(10L);
		assertThat(notification.getType()).isEqualTo(NotificationType.MENTION);
		assertThat(notification.isRead()).isFalse();
		assertThat(notification.getDeepLinkUrl()).isEqualTo("/projects/1/files/2?commentId=3");
		assertThat(notificationService.getUnreadCount(20L)).isEqualTo(1);
		verify(publisher).publishNotification(any(Notification.class), org.mockito.ArgumentMatchers.eq(1L));
	}

	@Test
	void sendBulkCreatesOneNotificationForEachRecipient() {
		BulkNotificationRequest request = new BulkNotificationRequest();
		request.setRecipientIds(Set.of(20L, 30L));
		request.setActorId(10L);
		request.setType(NotificationType.BROADCAST);
		request.setTitle("Platform update");
		request.setMessage("Maintenance tonight");

		assertThat(notificationService.sendBulk(request)).hasSize(2);
		assertThat(notificationService.getUnreadCount(20L)).isEqualTo(1);
		assertThat(notificationService.getUnreadCount(30L)).isEqualTo(1);
	}

	@Test
	void markReadAndMarkAllReadUpdateUnreadCounts() {
		Notification first = notificationService.send(request(20L, NotificationType.COMMENT));
		notificationService.send(request(20L, NotificationType.SNAPSHOT));

		notificationService.markAsRead(first.getNotificationId());

		assertThat(notificationService.getUnreadCount(20L)).isEqualTo(1);
		assertThat(notificationService.markAllRead(20L)).isEqualTo(1);
		assertThat(notificationService.getUnreadCount(20L)).isZero();
	}

	@Test
	void deleteReadRemovesOnlyReadNotifications() {
		Notification read = notificationService.send(request(20L, NotificationType.COMMENT));
		notificationService.markAsRead(read.getNotificationId());
		notificationService.send(request(20L, NotificationType.MENTION));

		assertThat(notificationService.deleteRead(20L)).isEqualTo(1);

		assertThat(notificationService.getByRecipient(20L)).hasSize(1);
		assertThat(notificationService.getUnreadCount(20L)).isEqualTo(1);
	}

	@Test
	void getByTypeAndRelatedIdReturnMatchingNotifications() {
		notificationService.send(request(20L, NotificationType.FORK));
		notificationService.send(request(30L, NotificationType.MENTION));

		assertThat(notificationService.getByType(NotificationType.FORK)).hasSize(1);
		assertThat(notificationService.getByRelatedId("3")).hasSize(2);
	}

	private SendNotificationRequest request(Long recipientId, NotificationType type) {
		SendNotificationRequest request = new SendNotificationRequest();
		request.setRecipientId(recipientId);
		request.setActorId(10L);
		request.setType(type);
		request.setTitle("Code review update");
		request.setMessage("You were mentioned in a comment");
		request.setRelatedId("3");
		request.setRelatedType("COMMENT");
		request.setDeepLinkUrl("/projects/1/files/2?commentId=3");
		return request;
	}
}
