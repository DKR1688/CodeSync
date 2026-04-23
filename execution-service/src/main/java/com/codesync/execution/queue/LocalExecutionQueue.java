package com.codesync.execution.queue;

import com.codesync.execution.service.ExecutionWorker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.Executor;

@Component
@ConditionalOnProperty(prefix = "codesync.execution.queue", name = "mode", havingValue = "local", matchIfMissing = true)
public class LocalExecutionQueue implements ExecutionQueue {

	private final Executor executionTaskExecutor;
	private final ExecutionWorker executionWorker;

	public LocalExecutionQueue(@Qualifier("executionTaskExecutor") Executor executionTaskExecutor,
			ExecutionWorker executionWorker) {
		this.executionTaskExecutor = executionTaskExecutor;
		this.executionWorker = executionWorker;
	}

	@Override
	public void enqueue(UUID jobId) {
		executionTaskExecutor.execute(() -> executionWorker.processJob(jobId));
	}
}
