package com.codesync.version.repository;

import com.codesync.version.entity.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {

	Optional<Snapshot> findBySnapshotId(Long snapshotId);

	List<Snapshot> findByProjectIdOrderByCreatedAtDescSnapshotIdDesc(Long projectId);

	List<Snapshot> findByFileIdOrderByCreatedAtDescSnapshotIdDesc(Long fileId);

	List<Snapshot> findByAuthorIdOrderByCreatedAtDescSnapshotIdDesc(Long authorId);

	List<Snapshot> findByProjectIdAndBranchOrderByCreatedAtDescSnapshotIdDesc(Long projectId, String branch);

	List<Snapshot> findByFileIdAndBranchOrderByCreatedAtDescSnapshotIdDesc(Long fileId, String branch);

	Optional<Snapshot> findFirstByFileIdOrderByCreatedAtDescSnapshotIdDesc(Long fileId);

	Optional<Snapshot> findFirstByFileIdAndBranchOrderByCreatedAtDescSnapshotIdDesc(Long fileId, String branch);

	List<Snapshot> findByHash(String hash);

	List<Snapshot> findByTag(String tag);

	boolean existsByProjectIdAndFileIdAndBranch(Long projectId, Long fileId, String branch);

	boolean existsByProjectIdAndTag(Long projectId, String tag);
}
