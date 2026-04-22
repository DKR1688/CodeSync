package com.codesync.file.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
	}

	@ExceptionHandler({ InvalidFileRequestException.class, IllegalArgumentException.class })
	public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
	}

	@ExceptionHandler(DownstreamServiceException.class)
	public ResponseEntity<ApiErrorResponse> handleDownstreamService(DownstreamServiceException ex,
			HttpServletRequest request) {
		return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.orElse("Validation failed");
		return buildResponse(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, HandlerMethodValidationException.class })
	public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(Exception ex, HttpServletRequest request) {
		String message = "Invalid request payload";
		if (ex instanceof HttpMessageNotReadableException readable && readable.getMostSpecificCause() != null) {
			message = readable.getMostSpecificCause().getMessage();
		}
		return buildResponse(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error while handling {} {}", request.getMethod(), request.getRequestURI(), ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
			HttpServletRequest request) {
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
	}
}
