package com.codesync.version.service;

import com.codesync.version.client.FileServiceClient;
import com.codesync.version.dto.CreateBranchRequest;
import com.codesync.version.dto.CreateSnapshotRequest;
import com.codesync.version.dto.DiffLine;
import com.codesync.version.dto.DiffOperation;
import com.codesync.version.dto.DiffResponse;
import com.codesync.version.dto.RestoreSnapshotRequest;
import com.codesync.version.entity.Snapshot;
import com.codesync.version.exception.InvalidVersionRequestException;
import com.codesync.version.exception.ResourceNotFoundException;
import com.codesync.version.repository.SnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class VersionServiceImpl implements VersionService {

	private static final String DEFAULT_BRANCH = "main";

	private final SnapshotRepository repository;
	private final FileServiceClient fileServiceClient;

	public VersionServiceImpl(SnapshotRepository repository, FileServiceClient fileServiceClient) {
		this.repository = repository;
		this.fileServiceClient = fileServiceClient;
	}

	@Override
	public Snapshot createSnapshot(CreateSnapshotRequest request, Long authorId) {
		if (request == null) {
			throw new InvalidVersionRequestException("Snapshot payload is required");
		}
		validatePositiveId(request.getProjectId(), "Project id");
		validatePositiveId(request.getFileId(), "File id");
		validatePositiveId(authorId, "Author id");

		String branch = normalizeBranch(request.getBranch());
		Long parentSnapshotId = resolveParentSnapshotId(request.getParentSnapshotId(), request.getProjectId(),
				request.getFileId(), branch);

		Snapshot snapshot = new Snapshot();
		snapshot.setProjectId(request.getProjectId());
		snapshot.setFileId(request.getFileId());
		snapshot.setAuthorId(authorId);
		snapshot.setMessage(normalizeMessage(request.getMessage(), "Snapshot created"));
		snapshot.setContent(request.getContent() == null ? "" : request.getContent());
		snapshot.setHash(sha256(snapshot.getContent()));
		snapshot.setParentSnapshotId(parentSnapshotId);
		snapshot.setBranch(branch);
		return repository.save(snapshot);
	}

	@Override
	@Transactional(readOnly = true)
	public Snapshot getSnapshotById(Long snapshotId) {
		validatePositiveId(snapshotId, "Snapshot id");
		return getSnapshotOrThrow(snapshotId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Snapshot> getSnapshotsByFile(Long fileId) {
		validatePositiveId(fileId, "File id");
		return repository.findByFileIdOrderByCreatedAtDescSnapshotIdDesc(fileId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Snapshot> getSnapshotsByProject(Long projectId) {
		validatePositiveId(projectId, "Project id");
		return repository.findByProjectIdOrderByCreatedAtDescSnapshotIdDesc(projectId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Snapshot> getSnapshotsByBranch(Long projectId, String branch) {
		validatePositiveId(projectId, "Project id");
		return repository.findByProjectIdAndBranchOrderByCreatedAtDescSnapshotIdDesc(projectId, normalizeBranch(branch));
	}

	@Override
	@Transactional(readOnly = true)
	public Snapshot getLatestSnapshot(Long fileId, String branch) {
		validatePositiveId(fileId, "File id");
		return findLatestSnapshot(fileId, normalizeBranch(branch))
				.orElseThrow(() -> new ResourceNotFoundException("No snapshots found for file " + fileId));
	}

	@Override
	public Snapshot restoreSnapshot(Long snapshotId, RestoreSnapshotRequest request, Long authorId,
			String authorizationHeader) {
		validatePositiveId(authorId, "Author id");
		Snapshot source = getSnapshotById(snapshotId);
		verifyIntegrity(source);

		String branch = request != null && StringUtils.hasText(request.getBranch())
				? normalizeRequiredBranch(request.getBranch())
				: source.getBranch();
		Long parentSnapshotId = findLatestSnapshot(source.getFileId(), branch)
				.map(Snapshot::getSnapshotId)
				.orElse(source.getSnapshotId());
		String message = normalizeMessage(request != null ? request.getMessage() : null,
				"Restore snapshot " + source.getSnapshotId());

		fileServiceClient.updateFileContent(source.getFileId(), source.getContent(), authorizationHeader);

		Snapshot restored = new Snapshot();
		restored.setProjectId(source.getProjectId());
		restored.setFileId(source.getFileId());
		restored.setAuthorId(authorId);
		restored.setMessage(message);
		restored.setContent(source.getContent());
		restored.setHash(sha256(source.getContent()));
		restored.setParentSnapshotId(parentSnapshotId);
		restored.setBranch(branch);
		return repository.save(restored);
	}

	@Override
	@Transactional(readOnly = true)
	public DiffResponse diffSnapshots(Long fromSnapshotId, Long toSnapshotId) {
		Snapshot from = getSnapshotById(fromSnapshotId);
		Snapshot to = getSnapshotById(toSnapshotId);
		if (!from.getFileId().equals(to.getFileId())) {
			throw new InvalidVersionRequestException("Snapshots must belong to the same file to compute a diff");
		}
		verifyIntegrity(from);
		verifyIntegrity(to);

		List<DiffLine> lines = computeLineDiff(splitLines(from.getContent()), splitLines(to.getContent()));
		int added = (int) lines.stream().filter(line -> line.getOperation() == DiffOperation.ADD).count();
		int removed = (int) lines.stream().filter(line -> line.getOperation() == DiffOperation.REMOVE).count();
		return new DiffResponse(from.getSnapshotId(), to.getSnapshotId(), from.getHash(), to.getHash(),
				added, removed, lines);
	}

	@Override
	public Snapshot createBranch(CreateBranchRequest request, Long authorId) {
		if (request == null) {
			throw new InvalidVersionRequestException("Branch payload is required");
		}
		validatePositiveId(authorId, "Author id");
		Snapshot source = getSnapshotById(request.getSourceSnapshotId());
		verifyIntegrity(source);
		String branch = normalizeRequiredBranch(request.getBranch());
		if (repository.existsByProjectIdAndFileIdAndBranch(source.getProjectId(), source.getFileId(), branch)) {
			throw new InvalidVersionRequestException("Branch already exists for this file: " + branch);
		}

		Snapshot branchHead = new Snapshot();
		branchHead.setProjectId(source.getProjectId());
		branchHead.setFileId(source.getFileId());
		branchHead.setAuthorId(authorId);
		branchHead.setMessage(normalizeMessage(request.getMessage(),
				"Create branch " + branch + " from snapshot " + source.getSnapshotId()));
		branchHead.setContent(source.getContent());
		branchHead.setHash(sha256(source.getContent()));
		branchHead.setParentSnapshotId(source.getSnapshotId());
		branchHead.setBranch(branch);
		return repository.save(branchHead);
	}

	@Override
	public Snapshot tagSnapshot(Long snapshotId, String tag) {
		Snapshot snapshot = getSnapshotById(snapshotId);
		String normalizedTag = normalizeTag(tag);
		if (repository.existsByProjectIdAndTag(snapshot.getProjectId(), normalizedTag)
				&& !normalizedTag.equals(snapshot.getTag())) {
			throw new InvalidVersionRequestException("Tag already exists in this project: " + normalizedTag);
		}
		snapshot.setTag(normalizedTag);
		return repository.save(snapshot);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Snapshot> getFileHistory(Long fileId) {
		return getSnapshotsByFile(fileId);
	}

	private Long resolveParentSnapshotId(Long requestedParentId, Long projectId, Long fileId, String branch) {
		if (requestedParentId == null) {
			return findLatestSnapshot(fileId, branch).map(Snapshot::getSnapshotId).orElse(null);
		}

		validatePositiveId(requestedParentId, "Parent snapshot id");
		Snapshot parent = getSnapshotOrThrow(requestedParentId);
		if (!parent.getProjectId().equals(projectId) || !parent.getFileId().equals(fileId)) {
			throw new InvalidVersionRequestException("Parent snapshot must belong to the same project and file");
		}
		return parent.getSnapshotId();
	}

	private java.util.Optional<Snapshot> findLatestSnapshot(Long fileId, String branch) {
		if (StringUtils.hasText(branch)) {
			return repository.findFirstByFileIdAndBranchOrderByCreatedAtDescSnapshotIdDesc(fileId, branch);
		}
		return repository.findFirstByFileIdOrderByCreatedAtDescSnapshotIdDesc(fileId);
	}

	private Snapshot getSnapshotOrThrow(Long snapshotId) {
		return repository.findBySnapshotId(snapshotId)
				.orElseThrow(() -> new ResourceNotFoundException("Snapshot not found with id " + snapshotId));
	}

	private void verifyIntegrity(Snapshot snapshot) {
		String actualHash = sha256(snapshot.getContent() == null ? "" : snapshot.getContent());
		if (!actualHash.equals(snapshot.getHash())) {
			throw new InvalidVersionRequestException(
					"Snapshot " + snapshot.getSnapshotId() + " failed SHA-256 integrity verification");
		}
	}

	private List<String> splitLines(String content) {
		if (content == null || content.isEmpty()) {
			return List.of();
		}
		return Arrays.asList(content.split("\\R", -1));
	}

	private List<DiffLine> computeLineDiff(List<String> oldLines, List<String> newLines) {
		int oldSize = oldLines.size();
		int newSize = newLines.size();
		int[][] lcs = new int[oldSize + 1][newSize + 1];

		for (int oldIndex = oldSize - 1; oldIndex >= 0; oldIndex--) {
			for (int newIndex = newSize - 1; newIndex >= 0; newIndex--) {
				if (oldLines.get(oldIndex).equals(newLines.get(newIndex))) {
					lcs[oldIndex][newIndex] = lcs[oldIndex + 1][newIndex + 1] + 1;
				} else {
					lcs[oldIndex][newIndex] = Math.max(lcs[oldIndex + 1][newIndex], lcs[oldIndex][newIndex + 1]);
				}
			}
		}

		List<DiffLine> result = new ArrayList<>();
		int oldIndex = 0;
		int newIndex = 0;
		while (oldIndex < oldSize && newIndex < newSize) {
			if (oldLines.get(oldIndex).equals(newLines.get(newIndex))) {
				result.add(new DiffLine(DiffOperation.EQUAL, oldIndex + 1, newIndex + 1, oldLines.get(oldIndex)));
				oldIndex++;
				newIndex++;
			} else if (lcs[oldIndex + 1][newIndex] >= lcs[oldIndex][newIndex + 1]) {
				result.add(new DiffLine(DiffOperation.REMOVE, oldIndex + 1, null, oldLines.get(oldIndex)));
				oldIndex++;
			} else {
				result.add(new DiffLine(DiffOperation.ADD, null, newIndex + 1, newLines.get(newIndex)));
				newIndex++;
			}
		}
		while (oldIndex < oldSize) {
			result.add(new DiffLine(DiffOperation.REMOVE, oldIndex + 1, null, oldLines.get(oldIndex)));
			oldIndex++;
		}
		while (newIndex < newSize) {
			result.add(new DiffLine(DiffOperation.ADD, null, newIndex + 1, newLines.get(newIndex)));
			newIndex++;
		}
		return result;
	}

	private String sha256(String content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest((content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder(hashBytes.length * 2);
			for (byte hashByte : hashBytes) {
				builder.append(String.format("%02x", hashByte));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 algorithm not available", ex);
		}
	}

	private String normalizeBranch(String branch) {
		return StringUtils.hasText(branch) ? normalizeRequiredBranch(branch) : DEFAULT_BRANCH;
	}

	private String normalizeRequiredBranch(String branch) {
		String normalized = normalizeReference(branch, "Branch");
		if (!normalized.matches("[A-Za-z0-9._-]+")) {
			throw new InvalidVersionRequestException("Branch can only contain letters, numbers, dots, dashes, and underscores");
		}
		return normalized;
	}

	private String normalizeTag(String tag) {
		return normalizeReference(tag, "Tag");
	}

	private String normalizeReference(String value, String fieldName) {
		if (!StringUtils.hasText(value)) {
			throw new InvalidVersionRequestException(fieldName + " is required");
		}
		String normalized = value.trim();
		if (normalized.length() > 100) {
			throw new InvalidVersionRequestException(fieldName + " must be 100 characters or fewer");
		}
		return normalized;
	}

	private String normalizeMessage(String message, String defaultMessage) {
		String normalized = StringUtils.hasText(message) ? message.trim() : defaultMessage;
		if (normalized.length() > 1000) {
			throw new InvalidVersionRequestException("Message must be 1000 characters or fewer");
		}
		return normalized;
	}

	private void validatePositiveId(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new InvalidVersionRequestException(fieldName + " must be greater than 0");
		}
	}
}
