package com.codesync.file.repository;

import com.codesync.file.entity.CodeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<CodeFile, Long> {

	List<CodeFile> findByProjectIdOrderByPathAsc(Long projectId);

	List<CodeFile> findByProjectIdAndIsDeletedFalseOrderByPathAsc(Long projectId);

	Optional<CodeFile> findByFileId(Long fileId);

	Optional<CodeFile> findByProjectIdAndPathAndIsDeletedFalse(Long projectId, String path);

	List<CodeFile> findByLanguageIgnoreCaseAndIsDeletedFalse(String language);

	List<CodeFile> findByLastEditedByAndIsDeletedFalse(Long lastEditedBy);

	long countByProjectIdAndIsDeletedFalse(Long projectId);

	List<CodeFile> findByIsDeleted(boolean isDeleted);

	void deleteByFileId(Long fileId);
}
