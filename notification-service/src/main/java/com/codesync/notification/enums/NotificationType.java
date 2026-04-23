package com.codesync.notification.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationType {
	SESSION_INVITE,
	PARTICIPANT_JOINED,
	PARTICIPANT_LEFT,
	COMMENT,
	MENTION,
	SNAPSHOT,
	FORK,
	BROADCAST;

	@JsonCreator
	public static NotificationType fromValue(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim().replace('-', '_').replace(' ', '_');
		for (NotificationType type : values()) {
			if (type.name().equalsIgnoreCase(normalized)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unsupported notification type: " + value);
	}

	@JsonValue
	public String toValue() {
		return name();
	}
}
