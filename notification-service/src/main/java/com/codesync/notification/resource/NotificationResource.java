package com.codesync.notification.resource;

import com.codesync.notification.dto.BulkNotificationRequest;
import com.codesync.notification.dto.EmailNotificationRequest;
import com.codesync.notification.dto.SendNotificationRequest;
import com.codesync.notification.entity.Notification;
import com.codesync.notification.enums.NotificationType;
import com.codesync.notification.exception.InvalidNotificationRequestException;
import com.codesync.notification.security.AuthenticatedUser;
import com.codesync.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/notifications")
public class NotificationResource {

	private final NotificationService service;

	public NotificationResource(NotificationService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<Notification> send(@Valid @RequestBody SendNotificationRequest request,
			Authentication authentication) {
		Long currentUserId = requireCurrentUserId(authentication);
		if (!currentUserId.equals(request.getActorId()) && !isAdmin(authentication)) {
			throw new AccessDeniedException("Actor id must match the authenticated user");
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(service.send(request));
	}

	@PostMapping("/bulk")
	public ResponseEntity<List<Notification>> sendBulk(@Valid @RequestBody BulkNotificationRequest request,
			Authentication authentication) {
		requireAdmin(authentication);
		return ResponseEntity.status(HttpStatus.CREATED).body(service.sendBulk(request));
	}

	@PostMapping("/email")
	public ResponseEntity<Void> sendEmail(@Valid @RequestBody EmailNotificationRequest request,
			Authentication authentication) {
		requireAdmin(authentication);
		service.sendEmail(request);
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/{id}")
	public Notification getById(@PathVariable Long id, Authentication authentication) {
		Notification notification = service.getById(id);
		assertRecipientOrAdmin(notification.getRecipientId(), authentication);
		return notification;
	}

	@GetMapping("/recipient/{recipientId}")
	public List<Notification> getByRecipient(@PathVariable Long recipientId, Authentication authentication) {
		assertRecipientOrAdmin(recipientId, authentication);
		return service.getByRecipient(recipientId);
	}

	@GetMapping("/recipient/{recipientId}/unread-count")
	public Map<String, Long> getUnreadCount(@PathVariable Long recipientId, Authentication authentication) {
		assertRecipientOrAdmin(recipientId, authentication);
		return Map.of("recipientId", recipientId, "unreadCount", service.getUnreadCount(recipientId));
	}

	@PutMapping("/{id}/read")
	public Notification markAsRead(@PathVariable Long id, Authentication authentication) {
		Notification notification = service.getById(id);
		assertRecipientOrAdmin(notification.getRecipientId(), authentication);
		return service.markAsRead(id);
	}

	@PutMapping("/recipient/{recipientId}/read-all")
	public Map<String, Integer> markAllRead(@PathVariable Long recipientId, Authentication authentication) {
		assertRecipientOrAdmin(recipientId, authentication);
		return Map.of("updatedCount", service.markAllRead(recipientId));
	}

	@DeleteMapping("/recipient/{recipientId}/read")
	public Map<String, Integer> deleteRead(@PathVariable Long recipientId, Authentication authentication) {
		assertRecipientOrAdmin(recipientId, authentication);
		return Map.of("deletedCount", service.deleteRead(recipientId));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
		Notification notification = service.getById(id);
		assertRecipientOrAdmin(notification.getRecipientId(), authentication);
		service.deleteNotification(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/all")
	public List<Notification> getAll(Authentication authentication) {
		requireAdmin(authentication);
		return service.getAll();
	}

	@GetMapping("/type/{type}")
	public List<Notification> getByType(@PathVariable NotificationType type, Authentication authentication) {
		requireAdmin(authentication);
		return service.getByType(type);
	}

	@GetMapping("/related/{relatedId}")
	public List<Notification> getByRelatedId(@PathVariable String relatedId, Authentication authentication) {
		requireAdmin(authentication);
		return service.getByRelatedId(relatedId);
	}

	private void assertRecipientOrAdmin(Long recipientId, Authentication authentication) {
		validatePositiveId(recipientId, "Recipient id");
		Long currentUserId = requireCurrentUserId(authentication);
		if (!recipientId.equals(currentUserId) && !isAdmin(authentication)) {
			throw new AccessDeniedException("You can only access your own notifications");
		}
	}

	private void requireAdmin(Authentication authentication) {
		requireCurrentUserId(authentication);
		if (!isAdmin(authentication)) {
			throw new AccessDeniedException("Admin access is required");
		}
	}

	private Long requireCurrentUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("Authentication is required");
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof AuthenticatedUser user) {
			return user.userId();
		}
		String name = authentication.getName();
		try {
			return Long.valueOf(name);
		} catch (NumberFormatException ex) {
			throw new AccessDeniedException("Unable to determine the authenticated user");
		}
	}

	private boolean isAdmin(Authentication authentication) {
		Object principal = authentication != null ? authentication.getPrincipal() : null;
		if (principal instanceof AuthenticatedUser user) {
			return user.isAdmin();
		}
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
	}

	private void validatePositiveId(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new InvalidNotificationRequestException(fieldName + " must be greater than 0");
		}
	}
}
