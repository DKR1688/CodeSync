package com.codesync.file.dto;

public class FileContentUpdateRequest {

	private String content;
	private boolean liveCollaboration;

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isLiveCollaboration() {
		return liveCollaboration;
	}

	public void setLiveCollaboration(boolean liveCollaboration) {
		this.liveCollaboration = liveCollaboration;
	}
}
