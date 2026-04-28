package com.codesync.execution.service;

import com.codesync.execution.entity.SupportedLanguage;
import com.codesync.execution.repository.SupportedLanguageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportedLanguageSeederTest {

	@Mock
	private SupportedLanguageRepository repository;

	@Test
	void seedsAllCaseStudyLanguagesWhenRepositoryIsEmpty() throws Exception {
		when(repository.count()).thenReturn(0L);
		SupportedLanguageSeeder seeder = new SupportedLanguageSeeder(repository);

		seeder.run(new DefaultApplicationArguments(new String[0]));

		ArgumentCaptor<List<SupportedLanguage>> captor = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(captor.capture());
		List<SupportedLanguage> seeded = captor.getValue();

		assertThat(seeded).hasSize(13);
		assertThat(seeded)
				.extracting(SupportedLanguage::getLanguage)
				.containsExactly(
						"python",
						"java",
						"javascript",
						"c",
						"cpp",
						"go",
						"rust",
						"ruby",
						"typescript",
						"php",
						"kotlin",
						"swift",
						"r");
		assertThat(seeded)
				.allSatisfy(language -> {
					assertThat(language.isEnabled()).isTrue();
					assertThat(language.getDefaultTimeLimitSeconds()).isEqualTo(20);
					assertThat(language.getDefaultMemoryLimitMb()).isEqualTo(256);
				});
	}

	@Test
	void doesNotSeedWhenLanguagesAlreadyExist() throws Exception {
		when(repository.count()).thenReturn(3L);
		SupportedLanguageSeeder seeder = new SupportedLanguageSeeder(repository);

		seeder.run(new DefaultApplicationArguments(new String[0]));

		verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
	}
}
