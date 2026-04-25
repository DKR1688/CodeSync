package com.codesync.execution.sandbox;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.enums.ExecutionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class DockerSandboxRunner implements SandboxRunner {

	private final boolean dockerEnabled;
	private final ExecutionProcessManager processManager;

	public DockerSandboxRunner(@Value("${codesync.execution.docker.enabled:true}") boolean dockerEnabled,
			ExecutionProcessManager processManager) {
		this.dockerEnabled = dockerEnabled;
		this.processManager = processManager;
	}

	@Override
	public SandboxExecutionResult run(ExecutionJob job, SupportedLanguage language, Consumer<String> stdoutConsumer) {
		if (!dockerEnabled) {
			return new SandboxExecutionResult(ExecutionStatus.FAILED, "",
					"Docker sandbox execution is disabled in this environment.", null,
					0, 0);
		}

		String imagePreparationError = ensureImageAvailable(language.getDockerImage());
		if (imagePreparationError != null) {
			return new SandboxExecutionResult(ExecutionStatus.FAILED, "",
					"Unable to prepare Docker image " + language.getDockerImage() + ": " + imagePreparationError,
					null, 0, 0);
		}

		long started = System.nanoTime();
		Path workDir = null;
		ExecutorService streamExecutor = Executors.newFixedThreadPool(2);
		try {
			workDir = Files.createTempDirectory("codesync-exec-");
			Files.writeString(workDir.resolve(language.getSourceFileName()),
					job.getSourceCode() == null ? "" : job.getSourceCode(), StandardCharsets.UTF_8);

			Process process = new ProcessBuilder(buildDockerCommand(job, language, workDir))
					.redirectErrorStream(false)
					.start();
			processManager.register(job.getJobId(), process);

			Future<String> stdoutFuture = streamExecutor.submit(capture(process.inputReader(StandardCharsets.UTF_8),
					stdoutConsumer));
			Future<String> stderrFuture = streamExecutor.submit(capture(process.errorReader(StandardCharsets.UTF_8),
					null));

			writeStdin(process, job.getStdin());

			boolean finished = process.waitFor(job.getTimeLimitSeconds(), TimeUnit.SECONDS);
			if (!finished) {
				process.destroyForcibly();
				String stdout = getCaptured(stdoutFuture);
				String stderr = appendLine(getCaptured(stderrFuture), "Execution timed out after "
						+ job.getTimeLimitSeconds() + " seconds.");
				return new SandboxExecutionResult(ExecutionStatus.TIMED_OUT, stdout, stderr, null,
						elapsedMillis(started), 0);
			}

			String stdout = getCaptured(stdoutFuture);
			String stderr = getCaptured(stderrFuture);
			if (processManager.isCancelRequested(job.getJobId())) {
				return new SandboxExecutionResult(ExecutionStatus.CANCELLED, stdout,
						appendLine(stderr, "Execution was cancelled."), null, elapsedMillis(started), 0);
			}

			int exitCode = process.exitValue();
			ExecutionStatus status = exitCode == 0 ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED;
			return new SandboxExecutionResult(status, stdout, stderr, exitCode, elapsedMillis(started), 0);
		} catch (IOException ex) {
			return new SandboxExecutionResult(ExecutionStatus.FAILED, "",
					"Unable to start Docker sandbox: " + ex.getMessage(), null, elapsedMillis(started), 0);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return new SandboxExecutionResult(ExecutionStatus.CANCELLED, "", "Execution worker was interrupted.",
					null, elapsedMillis(started), 0);
		} finally {
			streamExecutor.shutdownNow();
			processManager.complete(job.getJobId());
			deleteDirectory(workDir);
		}
	}

	private String ensureImageAvailable(String dockerImage) {
		try {
			Process inspect = new ProcessBuilder("docker", "image", "inspect", dockerImage)
					.redirectErrorStream(true)
					.start();
			if (!inspect.waitFor(30, TimeUnit.SECONDS)) {
				inspect.destroyForcibly();
				return "timed out while checking whether the image is already available.";
			}

			if (inspect.exitValue() == 0) {
				return null;
			}

			Process pull = new ProcessBuilder("docker", "pull", dockerImage)
					.redirectErrorStream(true)
					.start();
			if (!pull.waitFor(10, TimeUnit.MINUTES)) {
				pull.destroyForcibly();
				return "timed out while downloading the Docker image.";
			}

			if (pull.exitValue() != 0) {
				String pullOutput = readProcessOutput(pull);
				return pullOutput.isBlank() ? "docker pull exited with a non-zero status." : pullOutput;
			}

			return null;
		} catch (IOException ex) {
			return ex.getMessage();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return "image preparation was interrupted.";
		}
	}

	private List<String> buildDockerCommand(ExecutionJob job, SupportedLanguage language, Path workDir) {
		List<String> command = new ArrayList<>();
		command.add("docker");
		command.add("run");
		command.add("--rm");
		command.add("--network");
		command.add("none");
		command.add("--cpus");
		command.add(String.valueOf(job.getCpuLimit()));
		command.add("--memory");
		command.add(job.getMemoryLimitMb() + "m");
		command.add("--memory-swap");
		command.add(job.getMemoryLimitMb() + "m");
		command.add("--pids-limit");
		command.add("64");
		command.add("--cap-drop");
		command.add("ALL");
		command.add("--security-opt");
		command.add("no-new-privileges");
		command.add("--read-only");
		command.add("--tmpfs");
		command.add("/tmp:rw,exec,nosuid,size=64m");
		command.add("-v");
		command.add(workDir.toAbsolutePath() + ":/tmp/codesync:rw");
		command.add("-w");
		command.add("/tmp/codesync");
		command.add("-i");
		command.add("--entrypoint");
		command.add("sh");
		command.add(language.getDockerImage());
		command.add("-lc");
		command.add(language.getRunCommand());
		return command;
	}

	private void writeStdin(Process process, String stdin) {
		try (OutputStream outputStream = process.getOutputStream()) {
			if (stdin != null && !stdin.isEmpty()) {
				outputStream.write(stdin.getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException ignored) {
			// The process may exit before consuming stdin; stdout/stderr still explain the result.
		}
	}

	private Callable<String> capture(BufferedReader reader, Consumer<String> streamConsumer) {
		return () -> {
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append(System.lineSeparator());
				if (streamConsumer != null) {
					streamConsumer.accept(line + System.lineSeparator());
				}
			}
			return builder.toString();
		};
	}

	private String getCaptured(Future<String> future) {
		try {
			return future.get(1, TimeUnit.SECONDS);
		} catch (Exception ex) {
			return "";
		}
	}

	private String appendLine(String value, String line) {
		String base = value == null ? "" : value;
		return base + line + System.lineSeparator();
	}

	private String readProcessOutput(Process process) {
		try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
			return reader.lines().collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException ex) {
			return "";
		}
	}

	private long elapsedMillis(long startedNanos) {
		return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
	}

	private void deleteDirectory(Path path) {
		if (path == null) {
			return;
		}
		try (Stream<Path> stream = Files.walk(path)) {
			stream.sorted(Comparator.reverseOrder()).forEach(candidate -> {
				try {
					Files.deleteIfExists(candidate);
				} catch (IOException ignored) {
					// Best-effort cleanup; Docker containers are already removed with --rm.
				}
			});
		} catch (IOException ignored) {
			// Best-effort cleanup.
		}
	}
}
