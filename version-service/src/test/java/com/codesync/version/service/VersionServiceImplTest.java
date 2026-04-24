package com.codesync.version.service;

import com.codesync.version.client.FileServiceClient;
import com.codesync.version.dto.CreateBranchRequest;
import com.codesync.version.dto.CreateSnapshotRequest;
import com.codesync.version.dto.DiffOperation;
import com.codesync.version.dto.DiffResponse;
import com.codesync.version.dto.RestoreSnapshotRequest;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.repository.SnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class VersionServiceImplTest {

	@Autowired
	private VersionService versionService;

	@Autowired
	private SnapshotRepository repository;

	@MockitoBean
	private FileServiceClient fileServiceClient;

	@BeforeEach
	void cleanRepository() {
		repository.deleteAll();
	}

	@Test
	void createSnapshotStoresHashAndLinksParentOnSameBranch() {
		Snapshot first = versionService.createSnapshot(request("first", "line1", null), 99L);
		Snapshot second = versionService.createSnapshot(request("second", "line1\nline2", null), 99L);

		assertThat(first.getHash()).hasSize(64);
		assertThat(first.getBranch()).isEqualTo("main");
		assertThat(second.getParentSnapshotId()).isEqualTo(first.getSnapshotId());
		assertThat(repository.findByFileIdOrderByCreatedAtDescSnapshotIdDesc(20L)).hasSize(2);
	}

	@Test
	void diffSnapshotsReturnsLineAnnotations() {
		Snapshot first = versionService.createSnapshot(request("first", "a\nb\nc", null), 99L);
		Snapshot second = versionService.createSnapshot(request("second", "a\nx\nc\nd", null), 99L);

		DiffResponse diff = versionService.diffSnapshots(first.getSnapshotId(), second.getSnapshotId());

		assertThat(diff.getAddedLines()).isEqualTo(2);
		assertThat(diff.getRemovedLines()).isEqualTo(1);
		assertThat(diff.getLines()).extracting("operation")
				.contains(DiffOperation.REMOVE, DiffOperation.ADD);
	}

	@Test
	void createBranchCreatesNewBranchHeadFromSourceSnapshot() {
		Snapshot source = versionService.createSnapshot(request("first", "content", null), 99L);
		CreateBranchRequest request = new CreateBranchRequest();
		request.setSourceSnapshotId(source.getSnapshotId());
		request.setBranch("feature-one");

		Snapshot branchHead = versionService.createBranch(request, 100L);

		assertThat(branchHead.getBranch()).isEqualTo("feature-one");
		assertThat(branchHead.getParentSnapshotId()).isEqualTo(source.getSnapshotId());
		assertThat(branchHead.getContent()).isEqualTo(source.getContent());
	}

	@Test
	void restoreSnapshotUpdatesFileAndCreatesNewSnapshotOnSourceBranch() {
		Snapshot source = versionService.createSnapshot(request("feature", "old content", "feature-one"), 99L);
		versionService.createSnapshot(request("latest", "new content", "feature-one"), 99L);
		RestoreSnapshotRequest request = new RestoreSnapshotRequest();

		Snapshot restored = versionService.restoreSnapshot(source.getSnapshotId(), request, 100L, "Bearer token");

		assertThat(restored.getBranch()).isEqualTo("feature-one");
		assertThat(restored.getContent()).isEqualTo("old content");
		verify(fileServiceClient).updateFileContent(20L, "old content", "Bearer token");
	}

	private CreateSnapshotRequest request(String message, String content, String branch) {
		CreateSnapshotRequest request = new CreateSnapshotRequest();
		request.setProjectId(10L);
		request.setFileId(20L);
		request.setMessage(message);
		request.setContent(content);
		request.setBranch(branch);
		return request;
	}
}
