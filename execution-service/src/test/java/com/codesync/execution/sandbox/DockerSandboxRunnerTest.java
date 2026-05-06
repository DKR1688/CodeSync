package com.codesync.execution.sandbox;

import com.codesync.execution.entity.SupportedLanguage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DockerSandboxRunnerTest {

	@Test
	void normalizeStdinReturnsEmptyStringWhenInputIsMissing() {
		assertThat(DockerSandboxRunner.normalizeStdin(null)).isEmpty();
		assertThat(DockerSandboxRunner.normalizeStdin("")).isEmpty();
	}

	@Test
	void normalizeStdinAppendsTrailingLineBreakForSingleLineInput() {
		assertThat(DockerSandboxRunner.normalizeStdin("Jyoti"))
				.isEqualTo("Jyoti" + System.lineSeparator());
	}

	@Test
	void normalizeStdinPreservesExistingLineBreaks() {
		assertThat(DockerSandboxRunner.normalizeStdin("Jyoti\n")).isEqualTo("Jyoti\n");
		assertThat(DockerSandboxRunner.normalizeStdin("Jyoti\r\n")).isEqualTo("Jyoti\r\n");
	}

	@Test
	void resolveRunCommandUsesActualJavaSourceFileNameAndClassName() {
		SupportedLanguage language = new SupportedLanguage();
		language.setSourceFileName("Main.java");
		language.setRunCommand("javac Main.java && java Main");

		assertThat(DockerSandboxRunner.resolveRunCommand(language, "main.java"))
				.isEqualTo("javac main.java && java main");
	}

	@Test
	void resolveRunCommandSupportsTemplatePlaceholders() {
		SupportedLanguage language = new SupportedLanguage();
		language.setSourceFileName("main.py");
		language.setRunCommand("python3 {{sourceFileName}}");

		assertThat(DockerSandboxRunner.resolveRunCommand(language, "solver.py"))
				.isEqualTo("python3 solver.py");
	}
}
