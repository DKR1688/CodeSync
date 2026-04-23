package com.codesync.collab.repository;

import com.codesync.collab.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

	List<Participant> findBySessionSessionIdOrderByJoinedAtAsc(String sessionId);

	List<Participant> findBySessionSessionIdAndLeftAtIsNullOrderByJoinedAtAsc(String sessionId);

	Optional<Participant> findBySessionSessionIdAndUserIdAndLeftAtIsNull(String sessionId, Long userId);

	long countBySessionSessionIdAndLeftAtIsNull(String sessionId);
}
