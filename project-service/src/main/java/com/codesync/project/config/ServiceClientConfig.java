package com.codesync.project.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientConfig {

	@Bean
	@LoadBalanced
	public RestClient.Builder loadBalancedRestClientBuilder() {
		return RestClient.builder();
	}

	@Bean
	@Primary
	public RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}
}
