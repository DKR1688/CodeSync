package com.codesync.execution.service;

import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.repository.SupportedLanguageRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SupportedLanguageSeeder implements ApplicationRunner {

	private final SupportedLanguageRepository repository;

	public SupportedLanguageSeeder(SupportedLanguageRepository repository) {
		this.repository = repository;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (repository.count() > 0) {
			return;
		}
		repository.saveAll(defaultLanguages());
	}

	private List<SupportedLanguage> defaultLanguages() {
		return List.of(
				language("python", "Python", "3.12", "python:3.12-alpine", "main.py", "python3 main.py"),
				language("java", "Java", "21", "eclipse-temurin:21-jdk", "Main.java",
						"javac Main.java && java Main"),
				language("javascript", "JavaScript (Node.js)", "22", "node:22-alpine", "main.js", "node main.js"),
				language("c", "C", "GCC latest", "gcc:latest", "main.c", "gcc main.c -O2 -o main && ./main"),
				language("cpp", "C++", "GCC latest", "gcc:latest", "main.cpp", "g++ main.cpp -O2 -o main && ./main"),
				language("go", "Go", "1.24", "golang:1.24-alpine", "main.go", "go run main.go"),
				language("rust", "Rust", "latest", "rust:latest", "main.rs", "rustc main.rs -O -o main && ./main"),
				language("ruby", "Ruby", "3.4", "ruby:3.4-alpine", "main.rb", "ruby main.rb"),
				language("typescript", "TypeScript", "Deno latest", "denoland/deno:alpine", "main.ts",
						"deno run main.ts"),
				language("php", "PHP", "8.4", "php:8.4-cli-alpine", "main.php", "php main.php"),
				language("kotlin", "Kotlin", "latest", "zenika/kotlin:latest", "Main.kt",
						"kotlinc Main.kt -include-runtime -d main.jar && java -jar main.jar"),
				language("swift", "Swift", "latest", "swift:latest", "main.swift", "swift main.swift"),
				language("r", "R", "latest", "r-base:latest", "main.R", "Rscript main.R"));
	}

	private SupportedLanguage language(String key, String displayName, String version, String image,
			String sourceFileName, String runCommand) {
		SupportedLanguage language = new SupportedLanguage();
		language.setLanguage(key);
		language.setDisplayName(displayName);
		language.setRuntimeVersion(version);
		language.setDockerImage(image);
		language.setSourceFileName(sourceFileName);
		language.setRunCommand(runCommand);
		language.setEnabled(true);
		language.setDefaultTimeLimitSeconds(20);
		language.setDefaultMemoryLimitMb(256);
		return language;
	}
}
