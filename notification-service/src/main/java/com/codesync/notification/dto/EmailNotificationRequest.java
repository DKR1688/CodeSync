package com.codesync.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmailNotificationRequest {

	@NotBlank
	@Email
	private String to;

	@NotBlank
	@Size(max = 200)
	private String subject;

	@NotBlank
	@Size(max = 5000)
	private String body;

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
