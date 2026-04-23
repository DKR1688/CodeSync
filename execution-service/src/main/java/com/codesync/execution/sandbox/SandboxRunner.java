package com.codesync.execution.sandbox;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.SupportedLanguage;

import java.util.function.Consumer;

public interface SandboxRunner {

	SandboxExecutionResult run(ExecutionJob job, SupportedLanguage language, Consumer<String> stdoutConsumer);
}
