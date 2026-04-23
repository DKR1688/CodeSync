package com.codesync.execution.queue;

import java.util.UUID;

public interface ExecutionQueue {

	void enqueue(UUID jobId);
}
