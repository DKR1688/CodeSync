package com.codesync.notification.service;

import com.codesync.notification.dto.BulkNotificationRequest;
import com.codesync.notification.dto.EmailNotificationRequest;
import com.codesync.notification.dto.SendNotificationRequest;
import com.codesync.notification.entity.Notification;
import com.codesync.notification.enums.NotificationType;
import com.codesync.notification.exception.InvalidNotificationRequestException;
import com.codesync.notification.exception.ResourceNotFoundException;
import com.codesync.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);

	private final NotificationRepository repository;
	private final NotificationPublisher publisher;
	private final boolean emailEnabled;

	public NotificationServiceImpl(NotificationRepository repository, NotificationPublisher publisher,
			@Value("${notification.email.enabled:false}") boolean emailEnabled) {
		this.repository = repository;
		this.publisher = publisher;
		this.emailEnabled = emailEnabled;
	}

	@Override
	public Notification send(SendNotificationRequest request) {
		if (request == null) {
			throw new InvalidNotificationRequestException("Notification payload is required");
		}
		Notification notification = toNotification(request);
		Notification saved = repository.save(notification);
		publisher.publishNotification(saved, getUnreadCount(saved.getRecipientId()));
		return saved;
	}

	@Override
	public List<Notification> sendBulk(BulkNotificationRequest request) {
		if (request == null) {
			throw new InvalidNotificationRequestException("Bulk notification payload is required");
		}
		if (request.getRecipientIds().isEmpty()) {
			throw new InvalidNotificationRequestException("At least one recipient is required");
		}
		validatePositiveId(request.getActorId(), "Actor id");
		validateRequiredText(request.getTitle(), "Title", 200);
		validateRequiredText(request.getMessage(), "Message", 2000);
		NotificationType type = request.getType() != null ? request.getType() : NotificationType.BROADCAST;

		List<Notification> saved = request.getRecipientIds().stream()
				.distinct()
				.map(recipientId -> {
					SendNotificationRequest single = new SendNotificationRequest();
					single.setRecipientId(recipientId);
					single.setActorId(request.getActorId());
					single.setType(type);
					single.setTitle(request.getTitle());
					single.setMessage(request.getMessage());
					single.setRelatedId(request.getRelatedId());
					single.setRelatedType(request.getRelatedType());
					single.setDeepLinkUrl(request.getDeepLinkUrl());
					return repository.save(toNotification(single));
				})
				.toList();

		saved.forEach(notification -> publisher.publishNotification(notification,
				getUnreadCount(notification.getRecipientId())));
		return saved;
	}

	@Override
	public Notification markAsRead(Long notificationId) {
		Notification notification = getById(notificationId);
		if (!notification.isRead()) {
			notification.setRead(true);
			notification = repository.save(notification);
			publisher.publishUnreadCount(notification.getRecipientId(), getUnreadCount(notification.getRecipientId()));
		}
		return notification;
	}

	@Override
	public int markAllRead(Long recipientId) {
		validatePositiveId(recipientId, "Recipient id");
		List<Notification> unread = repository.findByRecipientIdAndIsReadOrderByCreatedAtDescNotificationIdDesc(
				recipientId, false);
		unread.forEach(notification -> notification.setRead(true));
		repository.saveAll(unread);
		publisher.publishUnreadCount(recipientId, 0);
		return unread.size();
	}

	@Override
	public int deleteRead(Long recipientId) {
		validatePositiveId(recipientId, "Recipient id");
		List<Notification> read = repository.findByRecipientIdAndIsReadOrderByCreatedAtDescNotificationIdDesc(
				recipientId, true);
		repository.deleteAll(read);
		publisher.publishUnreadCount(recipientId, getUnreadCount(recipientId));
		return read.size();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> getByRecipient(Long recipientId) {
		validatePositiveId(recipientId, "Recipient id");
		return repository.findByRecipientIdOrderByCreatedAtDescNotificationIdDesc(recipientId);
	}

	@Override
	@Transactional(readOnly = true)
	public long getUnreadCount(Long recipientId) {
		validatePositiveId(recipientId, "Recipient id");
		return repository.countByRecipientIdAndIsRead(recipientId, false);
	}

	@Override
	public void deleteNotification(Long notificationId) {
		Notification notification = getById(notificationId);
		repository.deleteByNotificationId(notificationId);
		publisher.publishUnreadCount(notification.getRecipientId(), getUnreadCount(notification.getRecipientId()));
	}

	@Override
	public void sendEmail(EmailNotificationRequest request) {
		if (request == null) {
			throw new InvalidNotificationRequestException("Email payload is required");
		}
		validateRequiredText(request.getTo(), "Email recipient", 320);
		validateRequiredText(request.getSubject(), "Email subject", 200);
		validateRequiredText(request.getBody(), "Email body", 5000);
		if (!emailEnabled) {
			LOGGER.info("Email notifications disabled; skipped email to {} with subject '{}'",
					request.getTo(), request.getSubject());
			return;
		}
		LOGGER.info("Email notification queued to {} with subject '{}'", request.getTo(), request.getSubject());
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> getAll() {
		return repository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> getByType(NotificationType type) {
		if (type == null) {
			throw new InvalidNotificationRequestException("Notification type is required");
		}
		return repository.findByTypeOrderByCreatedAtDescNotificationIdDesc(type);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Notification> getByRelatedId(String relatedId) {
		if (!StringUtils.hasText(relatedId)) {
			throw new InvalidNotificationRequestException("Related id is required");
		}
		return repository.findByRelatedIdOrderByCreatedAtDescNotificationIdDesc(relatedId.trim());
	}

	@Override
	@Transactional(readOnly = true)
	public Notification getById(Long notificationId) {
		validatePositiveId(notificationId, "Notification id");
		return repository.findByNotificationId(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + notificationId));
	}

	private Notification toNotification(SendNotificationRequest request) {
		validatePositiveId(request.getRecipientId(), "Recipient id");
		validatePositiveId(request.getActorId(), "Actor id");
		if (request.getType() == null) {
			throw new InvalidNotificationRequestException("Notification type is required");
		}
		Notification notification = new Notification();
		notification.setRecipientId(request.getRecipientId());
		notification.setActorId(request.getActorId());
		notification.setType(request.getType());
		notification.setTitle(validateRequiredText(request.getTitle(), "Title", 200));
		notification.setMessage(validateRequiredText(request.getMessage(), "Message", 2000));
		notification.setRelatedId(normalizeOptional(request.getRelatedId(), "Related id", 100));
		notification.setRelatedType(normalizeOptional(request.getRelatedType(), "Related type", 100));
		notification.setDeepLinkUrl(normalizeOptional(request.getDeepLinkUrl(), "Deep-link URL", 1000));
		notification.setRead(false);
		return notification;
	}

	private String validateRequiredText(String value, String fieldName, int maxLength) {
		if (!StringUtils.hasText(value)) {
			throw new InvalidNotificationRequestException(fieldName + " is required");
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new InvalidNotificationRequestException(fieldName + " must be " + maxLength + " characters or fewer");
		}
		return normalized;
	}

	private String normalizeOptional(String value, String fieldName, int maxLength) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new InvalidNotificationRequestException(fieldName + " must be " + maxLength + " characters or fewer");
		}
		return normalized;
	}

	private void validatePositiveId(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new InvalidNotificationRequestException(fieldName + " must be greater than 0");
		}
	}
}
