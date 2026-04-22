package com.codesync.notification.entity;

import com.codesync.notification.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long notificationId;

	@Column(nullable = false)
	private Long recipientId;

	@Column(nullable = false)
	private Long actorId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private NotificationType type;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, length = 2000)
	private String message;

	@Column(length = 100)
	private String relatedId;

	@Column(length = 100)
	private String relatedType;

	@Column(length = 1000)
	private String deepLinkUrl;

	@Column(nullable = false)
	private boolean isRead = false;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public Notification() {
	}

	@PrePersist
	public void onCreate() {
		this.createdAt = LocalDateTime.now();
	}

	public Long getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(Long notificationId) {
		this.notificationId = notificationId;
	}

	public Long getRecipientId() {
		return recipientId;
	}

	public void setRecipientId(Long recipientId) {
		this.recipientId = recipientId;
	}

	public Long getActorId() {
		return actorId;
	}

	public void setActorId(Long actorId) {
		this.actorId = actorId;
	}

	public NotificationType getType() {
		return type;
	}

	public void setType(NotificationType type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getRelatedId() {
		return relatedId;
	}

	public void setRelatedId(String relatedId) {
		this.relatedId = relatedId;
	}

	public String getRelatedType() {
		return relatedType;
	}

	public void setRelatedType(String relatedType) {
		this.relatedType = relatedType;
	}

	public String getDeepLinkUrl() {
		return deepLinkUrl;
	}

	public void setDeepLinkUrl(String deepLinkUrl) {
		this.deepLinkUrl = deepLinkUrl;
	}

	public boolean isRead() {
		return isRead;
	}

	public void setRead(boolean read) {
		isRead = read;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	@Override
	public String toString() {
		return "Notification{" +
				"notificationId=" + notificationId +
				", recipientId=" + recipientId +
				", actorId=" + actorId +
				", type=" + type +
				", isRead=" + isRead +
				'}';
	}

	@Override
	public int hashCode() {
		return Objects.hash(notificationId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Notification other)) {
			return false;
		}
		return Objects.equals(notificationId, other.notificationId);
	}
}
