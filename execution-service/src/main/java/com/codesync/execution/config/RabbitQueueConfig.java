package com.codesync.execution.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "codesync.execution.queue", name = "mode", havingValue = "rabbit")
public class RabbitQueueConfig {

	@Bean
	public DirectExchange executionExchange(
			@Value("${codesync.execution.queue.exchange:execution.exchange}") String exchangeName) {
		return new DirectExchange(exchangeName, true, false);
	}

	@Bean
	public Queue executionQueue(@Value("${codesync.execution.queue.name:execution.jobs}") String queueName) {
		return new Queue(queueName, true);
	}

	@Bean
	public Binding executionBinding(Queue executionQueue, DirectExchange executionExchange,
			@Value("${codesync.execution.queue.routing-key:execution.jobs}") String routingKey) {
		return BindingBuilder.bind(executionQueue).to(executionExchange).with(routingKey);
	}
}
