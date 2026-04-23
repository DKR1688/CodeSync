package com.codesync.execution.queue;

import com.codesync.execution.service.ExecutionWorker;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "codesync.execution.queue", name = "mode", havingValue = "rabbit")
public class ExecutionRabbitListener {

	private final ExecutionWorker executionWorker;

	public ExecutionRabbitListener(ExecutionWorker executionWorker) {
		this.executionWorker = executionWorker;
	}

	@RabbitListener(queues = "${codesync.execution.queue.name:execution.jobs}")
	public void process(String jobId) {
		executionWorker.processJob(UUID.fromString(jobId));
	}
}
