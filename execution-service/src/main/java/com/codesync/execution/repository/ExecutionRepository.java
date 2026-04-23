package com.codesync.execution.repository;

import com.codesync.execution.entity.ExecutionJob;
import com.codesync.execution.enums.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExecutionRepository extends JpaRepository<ExecutionJob, UUID> {

	Optional<ExecutionJob> findByJobId(UUID jobId);

	List<ExecutionJob> findByUserIdOrderByCreatedAtDesc(Long userId);

	List<ExecutionJob> findByProjectIdOrderByCreatedAtDesc(Long projectId);

	List<ExecutionJob> findByStatusOrderByCreatedAtAsc(ExecutionStatus status);

	List<ExecutionJob> findByLanguageOrderByCreatedAtDesc(String language);

	List<ExecutionJob> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

	long countByUserId(Long userId);

	long countByStatus(ExecutionStatus status);

	long countByLanguage(String language);
}
