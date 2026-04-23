package com.codesync.execution.queue;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "codesync.execution.queue", name = "mode", havingValue = "rabbit")
public class RabbitExecutionQueue implements ExecutionQueue {

	private final RabbitTemplate rabbitTemplate;
	private final String exchangeName;
	private final String routingKey;

	public RabbitExecutionQueue(RabbitTemplate rabbitTemplate,
			@Value("${codesync.execution.queue.exchange:execution.exchange}") String exchangeName,
			@Value("${codesync.execution.queue.routing-key:execution.jobs}") String routingKey) {
		this.rabbitTemplate = rabbitTemplate;
		this.exchangeName = exchangeName;
		this.routingKey = routingKey;
	}

	@Override
	public void enqueue(UUID jobId) {
		rabbitTemplate.convertAndSend(exchangeName, routingKey, jobId.toString());
	}
}
