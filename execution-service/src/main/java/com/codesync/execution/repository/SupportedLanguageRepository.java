package com.codesync.execution.repository;

import com.codesync.execution.entity.SupportedLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportedLanguageRepository extends JpaRepository<SupportedLanguage, String> {

	List<SupportedLanguage> findByEnabledTrueOrderByDisplayNameAsc();

	List<SupportedLanguage> findAllByOrderByDisplayNameAsc();
}
