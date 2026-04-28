package com.codesync.execution.sandbox;

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
}
