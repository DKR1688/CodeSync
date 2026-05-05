package com.codesync.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

	@Bean
	public TopicExchange codesyncExchange(@Value("${codesync.rabbit.exchange:codesync.events}") String exchangeName) {
		return new TopicExchange(exchangeName, true, false);
	}

	@Bean
	public Queue notificationQueue(
			@Value("${codesync.rabbit.queue.notification:notification.events}") String queueName) {
		return new Queue(queueName, true);
	}

	@Bean
	public Binding notificationBinding(Queue notificationQueue, TopicExchange codesyncExchange,
			@Value("${codesync.rabbit.routing-key.notification-send:notification.send}") String routingKey) {
		return BindingBuilder.bind(notificationQueue).to(codesyncExchange).with(routingKey);
	}

	@Bean
	public MessageConverter jacksonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}
