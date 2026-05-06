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
		List<SupportedLanguage> defaults = defaultLanguages();
		List<SupportedLanguage> existingLanguages = repository.findAll();
		if (existingLanguages.isEmpty()) {
			repository.saveAll(defaults);
			return;
		}

		java.util.Map<String, SupportedLanguage> existingByLanguage = existingLanguages.stream()
				.collect(java.util.stream.Collectors.toMap(SupportedLanguage::getLanguage, language -> language));
		for (SupportedLanguage defaultLanguage : defaults) {
			SupportedLanguage existing = existingByLanguage.get(defaultLanguage.getLanguage());
			if (existing == null) {
				repository.save(defaultLanguage);
				continue;
			}

			boolean enabled = existing.isEnabled();
			existing.setDisplayName(defaultLanguage.getDisplayName());
			existing.setRuntimeVersion(defaultLanguage.getRuntimeVersion());
			existing.setDockerImage(defaultLanguage.getDockerImage());
			existing.setSourceFileName(defaultLanguage.getSourceFileName());
			existing.setRunCommand(defaultLanguage.getRunCommand());
			existing.setDefaultTimeLimitSeconds(defaultLanguage.getDefaultTimeLimitSeconds());
			existing.setDefaultMemoryLimitMb(defaultLanguage.getDefaultMemoryLimitMb());
			existing.setEnabled(enabled);
			repository.save(existing);
		}
	}

	private List<SupportedLanguage> defaultLanguages() {
		return List.of(
				language("python", "Python", "3.12", "python:3.12-alpine", "main.py",
						"python3 {{sourceFileName}}", 20, 256),
				language("java", "Java", "21", "eclipse-temurin:21-jdk", "Main.java",
						"javac {{sourceFileName}} && java {{sourceBaseName}}", 20, 256),
				language("javascript", "JavaScript (Node.js)", "22", "node:22-alpine", "main.js",
						"node {{sourceFileName}}", 20, 256),
				language("c", "C", "GCC latest", "gcc:latest", "main.c",
						"gcc {{sourceFileName}} -O2 -o main && ./main", 20, 256),
				language("cpp", "C++", "GCC latest", "gcc:latest", "main.cpp",
						"g++ {{sourceFileName}} -O2 -o main && ./main", 20, 256),
				language("go", "Go", "1.24", "golang:1.24-alpine", "main.go",
						"/usr/local/go/bin/go run {{sourceFileName}}", 45, 256),
				language("rust", "Rust", "latest", "rust:latest", "main.rs",
						"/usr/local/cargo/bin/rustc {{sourceFileName}} -O -o main && ./main", 20, 256),
				language("ruby", "Ruby", "3.4", "ruby:3.4-alpine", "main.rb", "ruby {{sourceFileName}}", 20, 256),
				language("typescript", "TypeScript", "Deno latest", "denoland/deno:alpine", "main.ts",
						"deno run {{sourceFileName}}", 20, 256),
				language("php", "PHP", "8.4", "php:8.4-cli-alpine", "main.php", "php {{sourceFileName}}", 20, 256),
				language("kotlin", "Kotlin", "latest", "zenika/kotlin:latest", "Main.kt",
						"/usr/lib/kotlinc/bin/kotlinc -J-Xms32m -J-Xmx320m {{sourceFileName}} -include-runtime -d main.jar && /usr/java/openjdk-12/bin/java -Xms32m -Xmx192m -jar main.jar",
						60, 512),
				language("swift", "Swift", "latest", "swift:latest", "main.swift", "swift {{sourceFileName}}", 20, 256),
				language("r", "R", "latest", "r-base:latest", "main.R", "Rscript {{sourceFileName}}", 20, 256));
	}

	private SupportedLanguage language(String key, String displayName, String version, String image,
			String sourceFileName, String runCommand, int defaultTimeLimitSeconds, int defaultMemoryLimitMb) {
		SupportedLanguage language = new SupportedLanguage();
		language.setLanguage(key);
		language.setDisplayName(displayName);
		language.setRuntimeVersion(version);
		language.setDockerImage(image);
		language.setSourceFileName(sourceFileName);
		language.setRunCommand(runCommand);
		language.setEnabled(true);
		language.setDefaultTimeLimitSeconds(defaultTimeLimitSeconds);
		language.setDefaultMemoryLimitMb(defaultMemoryLimitMb);
		return language;
	}
}
