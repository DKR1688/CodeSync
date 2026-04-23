package com.codesync.execution.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExecutionWorkerConfig {

	@Bean
	public Executor executionTaskExecutor(
			@Value("${codesync.execution.worker.core-pool-size:2}") int corePoolSize,
			@Value("${codesync.execution.worker.max-pool-size:4}") int maxPoolSize,
			@Value("${codesync.execution.worker.queue-capacity:100}") int queueCapacity) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		executor.setThreadNamePrefix("execution-worker-");
		executor.initialize();
		return executor;
	}
}
