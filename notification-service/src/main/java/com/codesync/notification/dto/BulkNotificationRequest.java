package com.codesync.notification.dto;

import com.codesync.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.Set;

public class BulkNotificationRequest {

	@NotEmpty
	private Set<@Positive Long> recipientIds = new LinkedHashSet<>();

	@NotNull
	@Positive
	private Long actorId;

	@NotNull
	private NotificationType type = NotificationType.BROADCAST;

	@NotBlank
	@Size(max = 200)
	private String title;

	@NotBlank
	@Size(max = 2000)
	private String message;

	@Size(max = 100)
	private String relatedId;

	@Size(max = 100)
	private String relatedType;

	@Size(max = 1000)
	private String deepLinkUrl;

	public Set<Long> getRecipientIds() {
		return recipientIds;
	}

	public void setRecipientIds(Set<Long> recipientIds) {
		this.recipientIds = recipientIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(recipientIds);
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
}
