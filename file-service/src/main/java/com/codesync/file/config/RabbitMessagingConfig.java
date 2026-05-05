package com.codesync.file.config;

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
	public MessageConverter jacksonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}
}
