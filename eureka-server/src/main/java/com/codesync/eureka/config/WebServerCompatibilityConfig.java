package com.codesync.eureka.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServerCompatibilityConfig {

	@Bean
	@ConditionalOnMissingBean(ServerProperties.class)
	public ServerProperties serverProperties() {
		return new ServerProperties();
	}
}
