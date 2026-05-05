package com.codesync.notification.service;

import com.codesync.notification.dto.SendNotificationRequest;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
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
