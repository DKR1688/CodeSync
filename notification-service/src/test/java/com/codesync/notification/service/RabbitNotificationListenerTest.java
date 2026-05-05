package com.codesync.notification.service;

import com.codesync.notification.dto.SendNotificationRequest;
import com.codesync.notification.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitNotificationListenerTest {

	@Mock
	private NotificationService notificationService;

	@Test
	void handleNotificationCommandDelegatesToNotificationService() {
		RabbitNotificationListener listener = new RabbitNotificationListener(notificationService);
		SendNotificationRequest request = new SendNotificationRequest();
		request.setRecipientId(2L);
		request.setActorId(1L);
		request.setType(NotificationType.MENTION);
		request.setTitle("Mention");
		request.setMessage("You were mentioned");

		listener.handleNotificationCommand(request);

		verify(notificationService).send(request);
	}
}
