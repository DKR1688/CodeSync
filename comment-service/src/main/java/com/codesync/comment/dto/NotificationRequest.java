package com.codesync.comment.dto;

public class NotificationRequest {

	private Long recipientId;
	private Long actorId;
	private String type;
	private String title;
	private String message;
	private String relatedId;
	private String relatedType;
	private String deepLinkUrl;

	public NotificationRequest() {
	}

	public NotificationRequest(Long recipientId, Long actorId, String type, String title, String message,
			String relatedId, String relatedType, String deepLinkUrl) {
		this.recipientId = recipientId;
		this.actorId = actorId;
		this.type = type;
		this.title = title;
		this.message = message;
		this.relatedId = relatedId;
		this.relatedType = relatedType;
		this.deepLinkUrl = deepLinkUrl;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
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
