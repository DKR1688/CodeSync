package com.codesync.execution.exception;

public class InvalidExecutionRequestException extends RuntimeException {

	public InvalidExecutionRequestException(String message) {
		super(message);
	}

	public InvalidExecutionRequestException(String message, Throwable cause) {
		super(message, cause);
	}
}
