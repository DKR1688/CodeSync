package com.codesync.notification.service;

import com.codesync.notification.dto.UnreadCountMessage;
import com.codesync.notification.entity.Notification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationPublisher {

	private final SimpMessagingTemplate messagingTemplate;

	public NotificationPublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	public void publishNotification(Notification notification, long unreadCount) {
		messagingTemplate.convertAndSend(notificationTopic(notification.getRecipientId()), notification);
		publishUnreadCount(notification.getRecipientId(), unreadCount);
	}

	public void publishUnreadCount(Long recipientId, long unreadCount) {
		messagingTemplate.convertAndSend(unreadTopic(recipientId), new UnreadCountMessage(recipientId, unreadCount));
	}

	private String notificationTopic(Long recipientId) {
		return "/topic/notifications/" + recipientId;
	}

	private String unreadTopic(Long recipientId) {
		return "/topic/notifications/" + recipientId + "/unread";
	}
}
