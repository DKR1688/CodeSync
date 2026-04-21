package com.codesync.version.service;

import com.codesync.version.dto.CreateBranchRequest;
import com.codesync.version.dto.CreateSnapshotRequest;
import com.codesync.version.dto.DiffResponse;
import com.codesync.version.dto.RestoreSnapshotRequest;
import com.codesync.version.entity.Snapshot;

import java.util.List;

public interface VersionService {

	Snapshot createSnapshot(CreateSnapshotRequest request, Long authorId);

	Snapshot getSnapshotById(Long snapshotId);

	List<Snapshot> getSnapshotsByFile(Long fileId);

	List<Snapshot> getSnapshotsByProject(Long projectId);

	List<Snapshot> getSnapshotsByBranch(Long projectId, String branch);

	Snapshot getLatestSnapshot(Long fileId, String branch);

	Snapshot restoreSnapshot(Long snapshotId, RestoreSnapshotRequest request, Long authorId, String authorizationHeader);

	DiffResponse diffSnapshots(Long fromSnapshotId, Long toSnapshotId);

	Snapshot createBranch(CreateBranchRequest request, Long authorId);

	Snapshot tagSnapshot(Long snapshotId, String tag);

	List<Snapshot> getFileHistory(Long fileId);
}
