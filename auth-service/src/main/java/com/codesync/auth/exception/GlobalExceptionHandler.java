package com.codesync.auth.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<Object> handleAuthException(AuthException ex) {
		logger.warn("Authentication error: {}", ex.getMessage(), ex);
		return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {
		logger.warn("Validation failed", ex);
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return buildResponse(message, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {
		logger.warn("Constraint violation", ex);
		String message = ex.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.collect(Collectors.joining("; "));
		return buildResponse(message, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex) {
		logger.warn("Authentication failure", ex);
		return buildResponse("Authentication failed", HttpStatus.UNAUTHORIZED);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		logger.warn("Data integrity violation", ex);
		String message = "Database error";
		if (ex.getRootCause() != null && ex.getRootCause().getMessage() != null) {
			message = ex.getRootCause().getMessage();
		}
		return buildResponse(message, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Object> handleInvalidJson(HttpMessageNotReadableException ex) {
		logger.warn("Malformed JSON request", ex);
		return buildResponse("Malformed JSON request", HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Object> handleMissingParameter(MissingServletRequestParameterException ex) {
		logger.warn("Missing request parameter", ex);
		return buildResponse(ex.getParameterName() + " parameter is missing", HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
		logger.warn("Access denied", ex);
		return buildResponse("Access denied", HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleGeneric(Exception ex) {
		logger.error("Unexpected server error", ex);
		String message = ex.getMessage() != null ? ex.getMessage() : "Something went wrong";
		return buildResponse(message, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private ResponseEntity<Object> buildResponse(String message, HttpStatus status) {
		return new ResponseEntity<>(new ErrorResponse(message, status.value(), LocalDateTime.now()), status);
	}
}