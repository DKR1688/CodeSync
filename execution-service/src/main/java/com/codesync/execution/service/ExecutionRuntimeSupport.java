package com.codesync.execution.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class ExecutionRuntimeSupport {

	private final boolean dockerEnabled;

	public ExecutionRuntimeSupport(@Value("${codesync.execution.docker.enabled:true}") boolean dockerEnabled) {
		this.dockerEnabled = dockerEnabled;
	}

	public boolean supportsLanguage(String language) {
		if (dockerEnabled) {
			return true;
		}
		return switch (normalize(language)) {
			case "python" -> hasAnyCommand("python3", "python");
			case "java" -> hasAllCommands("javac", "java");
			case "javascript" -> hasAnyCommand("node");
			case "c" -> hasAnyCommand("gcc");
			case "cpp" -> hasAnyCommand("g++");
			case "go" -> hasAnyCommand("go");
			case "rust" -> hasAnyCommand("rustc");
			case "ruby" -> hasAnyCommand("ruby");
			case "typescript" -> hasAllCommands("node", "tsc");
			case "php" -> hasAnyCommand("php");
			default -> false;
		};
	}

	private boolean hasAllCommands(String... commands) {
		for (String command : commands) {
			if (!hasAnyCommand(command)) {
				return false;
			}
		}
		return true;
	}

	private boolean hasAnyCommand(String... commands) {
		for (String command : commands) {
			if (commandExists(command)) {
				return true;
			}
		}
		return false;
	}

	private boolean commandExists(String command) {
		boolean windows = System.getProperty("os.name", "")
				.toLowerCase(Locale.ROOT)
				.contains("win");
		ProcessBuilder processBuilder = windows
				? new ProcessBuilder("where", command)
				: new ProcessBuilder("which", command);
		try {
			Process process = processBuilder.redirectErrorStream(true).start();
			if (!process.waitFor(5, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				return false;
			}
			return process.exitValue() == 0;
		} catch (IOException ex) {
			return false;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private String normalize(String language) {
		return language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
	}
}
