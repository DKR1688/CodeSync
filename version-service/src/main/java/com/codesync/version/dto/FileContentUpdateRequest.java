package com.codesync.version.dto;

public class FileContentUpdateRequest {

	private String content;

	public FileContentUpdateRequest() {
	}

	public FileContentUpdateRequest(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
