package com.codesync.collab.repository;

import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CollabSessionRepository extends JpaRepository<CollabSession, String> {

	List<CollabSession> findByProjectIdOrderByCreatedAtDesc(Long projectId);

	List<CollabSession> findByStatusOrderByCreatedAtDesc(SessionStatus status);

	List<CollabSession> findByStatusAndLastActivityAtBefore(SessionStatus status, LocalDateTime threshold);

	Optional<CollabSession> findFirstByFileIdAndStatusOrderByCreatedAtDesc(Long fileId, SessionStatus status);
}
