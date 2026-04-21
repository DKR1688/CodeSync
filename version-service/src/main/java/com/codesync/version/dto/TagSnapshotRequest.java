package com.codesync.version.dto;

import jakarta.validation.constraints.NotBlank;

public class TagSnapshotRequest {

	@NotBlank
	private String tag;

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
}
