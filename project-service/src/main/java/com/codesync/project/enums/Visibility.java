package com.codesync.project.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Visibility {
	PUBLIC,
	PRIVATE;

	@JsonCreator
	public static Visibility fromValue(String value) {
		if (value == null) {
			return null;
		}

		for (Visibility visibility : values()) {
			if (visibility.name().equalsIgnoreCase(value.trim())) {
				return visibility;
			}
		}

		throw new IllegalArgumentException("Visibility must be PUBLIC or PRIVATE");
	}

	@JsonValue
	public String toValue() {
		return name();
	}
}
