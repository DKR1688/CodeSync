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
	private final Path workspaceRoot;
	private final ExecutionProcessManager processManager;

	public DockerSandboxRunner(@Value("${codesync.execution.docker.enabled:true}") boolean dockerEnabled,
			@Value("${codesync.execution.workspace-root:./runtime/executions}") String workspaceRoot,
			ExecutionProcessManager processManager) {
		this.dockerEnabled = dockerEnabled;
		this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
		this.processManager = processManager;
	}

	@Override
	public SandboxExecutionResult run(ExecutionJob job, SupportedLanguage language, Consumer<String> stdoutConsumer) {
		return dockerEnabled
				? runInDocker(job, language, stdoutConsumer)
				: runLocally(job, language, stdoutConsumer);
	}

	private SandboxExecutionResult runInDocker(ExecutionJob job, SupportedLanguage language,
			Consumer<String> stdoutConsumer) {
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
			Files.createDirectories(workspaceRoot);
			workDir = Files.createTempDirectory(workspaceRoot, "codesync-exec-");
			String sourceFileName = resolveSourceFileName(job, language);
			Files.writeString(workDir.resolve(sourceFileName),
					job.getSourceCode() == null ? "" : job.getSourceCode(), StandardCharsets.UTF_8);

			Process process = new ProcessBuilder(buildDockerCommand(job, language, workDir, sourceFileName))
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

	private SandboxExecutionResult runLocally(ExecutionJob job, SupportedLanguage language,
			Consumer<String> stdoutConsumer) {
		long started = System.nanoTime();
		Path workDir = null;
		ExecutorService streamExecutor = Executors.newFixedThreadPool(2);
		try {
			Files.createDirectories(workspaceRoot);
			workDir = Files.createTempDirectory(workspaceRoot, "codesync-exec-");
			String sourceFileName = resolveSourceFileName(job, language);
			Files.writeString(workDir.resolve(sourceFileName),
					job.getSourceCode() == null ? "" : job.getSourceCode(), StandardCharsets.UTF_8);

			Process process = new ProcessBuilder(buildLocalCommand(language, sourceFileName))
					.directory(workDir.toFile())
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
					"Unable to start local execution sandbox: " + ex.getMessage(), null, elapsedMillis(started), 0);
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

	private List<String> buildDockerCommand(ExecutionJob job, SupportedLanguage language, Path workDir,
			String sourceFileName) {
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
		command.add("256");
		command.add("--cap-drop");
		command.add("ALL");
		command.add("--security-opt");
		command.add("no-new-privileges");
		command.add("--read-only");
		command.add("--tmpfs");
		command.add("/tmp:rw,exec,nosuid,size=32m");
		command.add("-e");
		command.add("HOME=/tmp/codesync");
		command.add("-e");
		command.add("XDG_CACHE_HOME=/tmp/codesync/.cache");
		command.add("-e");
		command.add("TMPDIR=/tmp/codesync/.tmp");
		command.add("-e");
		command.add("GOCACHE=/tmp/codesync/.cache/go-build");
		command.add("-e");
		command.add("SWIFTPM_MODULECACHE_OVERRIDE=/tmp/codesync/.cache/clang/ModuleCache");
		command.add("-v");
		command.add(workDir.toAbsolutePath() + ":/tmp/codesync:rw");
		command.add("-w");
		command.add("/tmp/codesync");
		command.add("-i");
		command.add("--entrypoint");
		command.add("sh");
		command.add(language.getDockerImage());
		command.add("-lc");
		command.add("mkdir -p /tmp/codesync/.cache/clang/ModuleCache /tmp/codesync/.cache/go-build /tmp/codesync/.tmp && "
				+ resolveRunCommand(language, sourceFileName));
		return command;
	}

	private List<String> buildLocalCommand(SupportedLanguage language, String sourceFileName) {
		String command = resolveRunCommand(language, sourceFileName);
		if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
			return List.of("cmd", "/c", command);
		}
		return List.of("sh", "-lc", command);
	}

	static String resolveRunCommand(SupportedLanguage language, String sourceFileName) {
		String fallbackSourceFileName = language.getSourceFileName();
		String resolvedCommand = language.getRunCommand();
		String sourceBaseName = stripExtension(sourceFileName);
		String fallbackBaseName = stripExtension(fallbackSourceFileName);

		resolvedCommand = resolvedCommand
				.replace("{{sourceFileName}}", sourceFileName)
				.replace("{{sourceBaseName}}", sourceBaseName)
				.replace("{{defaultSourceFileName}}", fallbackSourceFileName)
				.replace("{{defaultSourceBaseName}}", fallbackBaseName);

		if (!fallbackSourceFileName.equals(sourceFileName)) {
			resolvedCommand = resolvedCommand.replace(fallbackSourceFileName, sourceFileName);
		}
		if (!fallbackBaseName.equals(sourceBaseName)) {
			resolvedCommand = replaceWholeWord(resolvedCommand, fallbackBaseName, sourceBaseName);
		}
		return applyLanguageCommandOverrides(language.getLanguage(), resolvedCommand, sourceFileName);
	}

	private String resolveSourceFileName(ExecutionJob job, SupportedLanguage language) {
		if (job.getSourceFileName() == null || job.getSourceFileName().isBlank()) {
			return language.getSourceFileName();
		}
		return job.getSourceFileName().trim();
	}

	private static String stripExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
	}

	private static String replaceWholeWord(String value, String target, String replacement) {
		if (target.isBlank()) {
			return value;
		}
		return value.replaceAll("(?<![A-Za-z0-9_])" + java.util.regex.Pattern.quote(target) + "(?![A-Za-z0-9_])",
				java.util.regex.Matcher.quoteReplacement(replacement));
	}

	private static String applyLanguageCommandOverrides(String languageKey, String resolvedCommand,
			String sourceFileName) {
		if (languageKey == null || languageKey.isBlank()) {
			return resolvedCommand;
		}
		return switch (languageKey) {
			case "go" -> "GOMAXPROCS=1 GOFLAGS=-p=1 go run " + sourceFileName;
			case "kotlin" -> "kotlinc -J-Xms32m -J-Xmx320m "
					+ sourceFileName
					+ " -include-runtime -d main.jar && java -Xms32m -Xmx192m -jar main.jar";
			case "rust" -> "rustc " + sourceFileName + " -O -o main && ./main";
			default -> resolvedCommand;
		};
	}

	private void writeStdin(Process process, String stdin) {
		try (OutputStream outputStream = process.getOutputStream()) {
			String normalizedStdin = normalizeStdin(stdin);
			if (!normalizedStdin.isEmpty()) {
				outputStream.write(normalizedStdin.getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException ignored) {
			// The process may exit before consuming stdin; stdout/stderr still explain the result.
		}
	}

	static String normalizeStdin(String stdin) {
		if (stdin == null || stdin.isEmpty()) {
			return "";
		}
		if (stdin.endsWith("\n") || stdin.endsWith("\r")) {
			return stdin;
		}
		return stdin + System.lineSeparator();
	}

	private Callable<String> capture(BufferedReader reader, Consumer<String> streamConsumer) {
		return () -> {
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line).append(System.lineSeparator());
				if (streamConsumer != null) {
					try {
						streamConsumer.accept(line + System.lineSeparator());
					} catch (RuntimeException ignored) {
						// Live stream delivery is best-effort. Persisted stdout/stderr should still succeed.
					}
				}
			}
			return builder.toString();
		};
	}

	private String getCaptured(Future<String> future) {
		try {
			return future.get(5, TimeUnit.SECONDS);
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
