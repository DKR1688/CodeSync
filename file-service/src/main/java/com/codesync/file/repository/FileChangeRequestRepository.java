package com.codesync.file.repository;

import com.codesync.file.entity.FileChangeRequest;
import com.codesync.file.entity.FileChangeRequest.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileChangeRequestRepository extends JpaRepository<FileChangeRequest, Long> {

	List<FileChangeRequest> findByProjectIdAndStatusOrderByCreatedAtDesc(Long projectId, Status status);

	Optional<FileChangeRequest> findByChangeRequestId(Long changeRequestId);
}
