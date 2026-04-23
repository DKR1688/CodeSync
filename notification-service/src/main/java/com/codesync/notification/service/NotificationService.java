package com.codesync.notification.service;

import com.codesync.notification.dto.BulkNotificationRequest;
import com.codesync.notification.dto.EmailNotificationRequest;
import com.codesync.notification.dto.SendNotificationRequest;
import com.codesync.notification.entity.Notification;
import com.codesync.notification.enums.NotificationType;

import java.util.List;

public interface NotificationService {

	Notification send(SendNotificationRequest request);

	List<Notification> sendBulk(BulkNotificationRequest request);

	Notification markAsRead(Long notificationId);

	int markAllRead(Long recipientId);

	int deleteRead(Long recipientId);

	List<Notification> getByRecipient(Long recipientId);

	long getUnreadCount(Long recipientId);

	void deleteNotification(Long notificationId);

	void sendEmail(EmailNotificationRequest request);

	List<Notification> getAll();

	List<Notification> getByType(NotificationType type);

	List<Notification> getByRelatedId(String relatedId);

	Notification getById(Long notificationId);
}
