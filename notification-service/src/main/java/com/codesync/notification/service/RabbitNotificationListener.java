package com.codesync.notification.service;

import com.codesync.notification.dto.SendNotificationRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "codesync.rabbit.enabled", havingValue = "true")
public class RabbitNotificationListener {

	private final NotificationService notificationService;

	public RabbitNotificationListener(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@RabbitListener(queues = "${codesync.rabbit.queue.notification:notification.events}")
	public void handleNotificationCommand(SendNotificationRequest request) {
		notificationService.send(request);
	}
}
