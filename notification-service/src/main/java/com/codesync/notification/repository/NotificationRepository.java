package com.codesync.notification.repository;

import com.codesync.notification.entity.Notification;
import com.codesync.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	List<Notification> findByRecipientIdOrderByCreatedAtDescNotificationIdDesc(Long recipientId);

	List<Notification> findByRecipientIdAndIsReadOrderByCreatedAtDescNotificationIdDesc(Long recipientId,
			boolean isRead);

	long countByRecipientIdAndIsRead(Long recipientId, boolean isRead);

	List<Notification> findByTypeOrderByCreatedAtDescNotificationIdDesc(NotificationType type);

	List<Notification> findByRelatedIdOrderByCreatedAtDescNotificationIdDesc(String relatedId);

	Optional<Notification> findByNotificationId(Long notificationId);

	void deleteByNotificationId(Long notificationId);

	void deleteByRecipientIdAndIsRead(Long recipientId, boolean isRead);
}
