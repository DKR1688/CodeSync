package com.codesync.file.service;

import com.codesync.file.dto.FileTreeNode;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.InvalidFileRequestException;
import com.codesync.file.exception.ResourceNotFoundException;
import com.codesync.file.repository.FileRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class FileServiceImpl implements FileService {

	private static final String FILE_TREE_CACHE = "files.tree";

	private final FileRepository repository;
	private final FileEventPublisher eventPublisher;
	private final CacheManager cacheManager;

	public FileServiceImpl(FileRepository repository, FileEventPublisher eventPublisher, CacheManager cacheManager) {
		this.repository = repository;
		this.eventPublisher = eventPublisher;
		this.cacheManager = cacheManager;
	}

	@Override
	@CacheEvict(value = FILE_TREE_CACHE, allEntries = true)
	public CodeFile createFile(CodeFile file) {
		if (file == null) {
			throw new InvalidFileRequestException("File payload is required");
		}

		validatePositiveId(file.getProjectId(), "Project id");
		validatePositiveId(file.getCreatedById(), "Created by user id");

		String normalizedPath = normalizePath(file.getPath());
		assertNoActiveConflict(file.getProjectId(), normalizedPath, null);

		CodeFile codeFile = new CodeFile();
		codeFile.setProjectId(file.getProjectId());
		codeFile.setPath(normalizedPath);
		codeFile.setName(extractName(normalizedPath));
		codeFile.setLanguage(normalizeOptional(file.getLanguage()));
		if (!StringUtils.hasText(codeFile.getLanguage())) {
			throw new InvalidFileRequestException("Language is required for files");
		}

		String content = file.getContent() == null ? "" : file.getContent();
		codeFile.setContent(content);
		codeFile.setSize(computeSize(content));
		codeFile.setCreatedById(file.getCreatedById());
		codeFile.setLastEditedBy(file.getLastEditedBy() != null ? file.getLastEditedBy() : file.getCreatedById());
		codeFile.setDeleted(false);

		return repository.save(codeFile);
	}

	@Override
	@Transactional(readOnly = true)
	public CodeFile getFileById(Long fileId) {
		validatePositiveId(fileId, "File id");
		return getFileOrThrow(fileId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CodeFile> getFilesByProject(Long projectId) {
		validatePositiveId(projectId, "Project id");
		return repository.findByProjectIdAndIsDeletedFalseOrderByPathAsc(projectId);
	}

	@Override
	@Transactional(readOnly = true)
	public String getFileContent(Long fileId) {
		return getFileById(fileId).getContent();
	}

	@Override
	public CodeFile updateFileContent(Long fileId, String content, Long editorId) {
		validatePositiveId(editorId, "Editor user id");
		CodeFile file = requireActiveFile(fileId);
		if (file.isFolder()) {
			throw new InvalidFileRequestException("Folder content cannot be updated");
		}

		String updatedContent = content == null ? "" : content;
		file.setContent(updatedContent);
		file.setSize(computeSize(updatedContent));
		file.setLastEditedBy(editorId);
		CodeFile saved = repository.save(file);
		if (eventPublisher != null) {
			eventPublisher.publishFileUpdated(saved);
		}
		return saved;
	}

	@Override
	@CacheEvict(value = FILE_TREE_CACHE, allEntries = true)
	public CodeFile renameFile(Long fileId, String newName) {
		CodeFile target = requireActiveFile(fileId);
		String normalizedName = normalizeName(newName);
		String newPath = joinPath(parentPath(target.getPath()), normalizedName);
		return moveInternal(target, newPath);
	}

	@Override
	@CacheEvict(value = FILE_TREE_CACHE, allEntries = true)
	public void deleteFile(Long fileId) {
		CodeFile target = getFileOrThrow(fileId);
		List<CodeFile> impactedFiles = collectTargetAndDescendants(target);
		impactedFiles.forEach(file -> file.setDeleted(true));
		repository.saveAll(impactedFiles);
	}

	@Override
	@CacheEvict(value = FILE_TREE_CACHE, allEntries = true)
	public void restoreFile(Long fileId) {
		CodeFile target = getFileOrThrow(fileId);
		if (!target.isDeleted()) {
			return;
		}

		List<CodeFile> impactedFiles = collectTargetAndDescendants(target);
		assertNoConflictsForMappedPaths(impactedFiles, impactedFiles.stream()
				.collect(LinkedHashMap::new, (map, file) -> map.put(file, file.getPath()), Map::putAll));
		impactedFiles.forEach(file -> file.setDeleted(false));
		repository.saveAll(impactedFiles);
	}

	@Override
	@CacheEvict(value = FILE_TREE_CACHE, allEntries = true)
	public CodeFile moveFile(Long fileId, String newPath) {
		return moveInternal(requireActiveFile(fileId), normalizePath(newPath));
	}

	@Override
	@CacheEvict(value = FILE_TREE_CACHE, allEntries = true)
	public CodeFile createFolder(Long projectId, String folderPath, Long creatorId) {
		validatePositiveId(projectId, "Project id");
		validatePositiveId(creatorId, "Created by user id");

		String normalizedPath = normalizePath(folderPath);
		assertNoActiveConflict(projectId, normalizedPath, null);

		CodeFile folder = new CodeFile();
		folder.setProjectId(projectId);
		folder.setPath(normalizedPath);
		folder.setName(extractName(normalizedPath));
		folder.setLanguage(null);
		folder.setContent("");
		folder.setSize(0);
		folder.setCreatedById(creatorId);
		folder.setLastEditedBy(creatorId);
		folder.setDeleted(false);

		return repository.save(folder);
	}

	@Override
	@CacheEvict(value = FILE_TREE_CACHE, allEntries = true)
	public void copyProjectFiles(Long sourceProjectId, Long targetProjectId, Long actorUserId) {
		validatePositiveId(sourceProjectId, "Source project id");
		validatePositiveId(targetProjectId, "Target project id");
		validatePositiveId(actorUserId, "Actor user id");

		if (sourceProjectId.equals(targetProjectId)) {
			throw new InvalidFileRequestException("Source and target project ids must be different");
		}

		List<CodeFile> targetFiles = repository.findByProjectIdAndIsDeletedFalseOrderByPathAsc(targetProjectId);
		if (!targetFiles.isEmpty()) {
			throw new InvalidFileRequestException("Target project already contains files");
		}

		List<CodeFile> sourceFiles = repository.findByProjectIdAndIsDeletedFalseOrderByPathAsc(sourceProjectId);
		if (sourceFiles.isEmpty()) {
			return;
		}

		List<CodeFile> copiedFiles = sourceFiles.stream()
				.map(sourceFile -> copyForProject(sourceFile, targetProjectId, actorUserId))
				.toList();

		repository.saveAll(copiedFiles);
	}

	@Override
	@Transactional(readOnly = true)
	public List<FileTreeNode> getFileTree(Long projectId) {
		validatePositiveId(projectId, "Project id");
		Cache cache = cacheManager != null ? cacheManager.getCache(FILE_TREE_CACHE) : null;
		if (cache != null) {
			FileTreeCacheEntry cached = cache.get(projectId, FileTreeCacheEntry.class);
			if (cached != null) {
				return new ArrayList<>(cached.nodes());
			}
		}

		List<FileTreeNode> tree = buildFileTree(projectId);
		if (cache != null) {
			cache.put(projectId, new FileTreeCacheEntry(tree));
		}
		return tree;
	}

	private List<FileTreeNode> buildFileTree(Long projectId) {
		List<CodeFile> files = repository.findByProjectIdAndIsDeletedFalseOrderByPathAsc(projectId);

		Map<String, FileTreeNode> nodesByPath = new LinkedHashMap<>();
		Map<String, FileTreeNode> rootNodes = new LinkedHashMap<>();

		for (CodeFile file : files) {
			String[] parts = file.getPath().split("/");
			String currentPath = "";
			FileTreeNode parent = null;

			for (int index = 0; index < parts.length; index++) {
				String part = parts[index];
				currentPath = currentPath.isEmpty() ? part : currentPath + "/" + part;
				boolean isLeaf = index == parts.length - 1;
				boolean isFolder = isLeaf ? file.isFolder() : true;

				FileTreeNode node = nodesByPath.computeIfAbsent(currentPath,
						path -> new FileTreeNode(null, part, path, isFolder, isLeaf ? file.getLanguage() : null));
				if (isLeaf) {
					node.setFileId(file.getFileId());
					node.setFolder(file.isFolder());
					node.setLanguage(file.getLanguage());
				}

				if (parent == null) {
					rootNodes.putIfAbsent(currentPath, node);
				} else if (!parent.getChildren().contains(node)) {
					parent.getChildren().add(node);
				}
				parent = node;
			}
		}

		sortTree(rootNodes.values());
		return new ArrayList<>(rootNodes.values());
	}

	private record FileTreeCacheEntry(List<FileTreeNode> nodes) {
	}

	@Override
	@Transactional(readOnly = true)
	public List<CodeFile> searchInProject(Long projectId, String query) {
		validatePositiveId(projectId, "Project id");
		if (!StringUtils.hasText(query)) {
			throw new InvalidFileRequestException("Search query is required");
		}

		String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
		return repository.findByProjectIdAndIsDeletedFalseOrderByPathAsc(projectId).stream()
				.filter(file -> matchesQuery(file, normalizedQuery))
				.toList();
	}

	private CodeFile moveInternal(CodeFile target, String newPath) {
		if (target.getPath().equals(newPath)) {
			return target;
		}

		List<CodeFile> impactedFiles = collectTargetAndDescendants(target);
		Map<CodeFile, String> mappedPaths = mapToMovedPaths(target, newPath, impactedFiles);
		assertNoConflictsForMappedPaths(impactedFiles, mappedPaths);

		mappedPaths.forEach((file, path) -> {
			file.setPath(path);
			file.setName(extractName(path));
		});
		repository.saveAll(impactedFiles);
		return target;
	}

	private Map<CodeFile, String> mapToMovedPaths(CodeFile target, String newPath, List<CodeFile> impactedFiles) {
		Map<CodeFile, String> mappedPaths = new LinkedHashMap<>();
		String oldPath = target.getPath();

		for (CodeFile file : impactedFiles) {
			if (file.equals(target)) {
				mappedPaths.put(file, newPath);
				continue;
			}

			String suffix = file.getPath().substring(oldPath.length());
			mappedPaths.put(file, newPath + suffix);
		}
		return mappedPaths;
	}

	private void assertNoConflictsForMappedPaths(List<CodeFile> impactedFiles, Map<CodeFile, String> mappedPaths) {
		Long projectId = impactedFiles.getFirst().getProjectId();
		Set<Long> impactedIds = impactedFiles.stream().map(CodeFile::getFileId).collect(java.util.stream.Collectors.toSet());
		Set<String> newPaths = new LinkedHashSet<>(mappedPaths.values());

		if (newPaths.size() != mappedPaths.size()) {
			throw new InvalidFileRequestException("Move or rename would create duplicate paths");
		}

		List<CodeFile> projectFiles = repository.findByProjectIdOrderByPathAsc(projectId);
		for (CodeFile file : projectFiles) {
			if (!file.isDeleted() && !impactedIds.contains(file.getFileId()) && newPaths.contains(file.getPath())) {
				throw new InvalidFileRequestException("A file or folder already exists at path " + file.getPath());
			}
		}
	}

	private List<CodeFile> collectTargetAndDescendants(CodeFile target) {
		List<CodeFile> projectFiles = repository.findByProjectIdOrderByPathAsc(target.getProjectId());
		String prefix = target.getPath() + "/";
		return projectFiles.stream()
				.filter(file -> file.getPath().equals(target.getPath()) || file.getPath().startsWith(prefix))
				.sorted(Comparator.comparing(CodeFile::getPath))
				.toList();
	}

	private CodeFile requireActiveFile(Long fileId) {
		CodeFile file = getFileById(fileId);
		if (file.isDeleted()) {
			throw new InvalidFileRequestException("File or folder is deleted");
		}
		return file;
	}

	private CodeFile getFileOrThrow(Long fileId) {
		return repository.findByFileId(fileId)
				.orElseThrow(() -> new ResourceNotFoundException("File not found with id " + fileId));
	}

	private void assertNoActiveConflict(Long projectId, String path, Long ignoredFileId) {
		repository.findByProjectIdAndPathAndIsDeletedFalse(projectId, path)
				.filter(existing -> ignoredFileId == null || !existing.getFileId().equals(ignoredFileId))
				.ifPresent(existing -> {
					throw new InvalidFileRequestException("A file or folder already exists at path " + existing.getPath());
				});
	}

	private boolean matchesQuery(CodeFile file, String normalizedQuery) {
		return containsIgnoreCase(file.getName(), normalizedQuery)
				|| containsIgnoreCase(file.getPath(), normalizedQuery)
				|| containsIgnoreCase(file.getLanguage(), normalizedQuery)
				|| containsIgnoreCase(file.getContent(), normalizedQuery);
	}

	private boolean containsIgnoreCase(String value, String normalizedQuery) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
	}

	private String normalizePath(String rawPath) {
		if (!StringUtils.hasText(rawPath)) {
			throw new InvalidFileRequestException("Path is required");
		}

		String normalized = rawPath.trim().replace('\\', '/');
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		if (!StringUtils.hasText(normalized)) {
			throw new InvalidFileRequestException("Path is required");
		}

		String[] segments = normalized.split("/");
		List<String> cleanedSegments = new ArrayList<>();
		for (String segment : segments) {
			if (!StringUtils.hasText(segment)) {
				continue;
			}
			String trimmed = segment.trim();
			if (".".equals(trimmed) || "..".equals(trimmed)) {
				throw new InvalidFileRequestException("Path cannot contain relative navigation segments");
			}
			cleanedSegments.add(trimmed);
		}

		if (cleanedSegments.isEmpty()) {
			throw new InvalidFileRequestException("Path is required");
		}
		return String.join("/", cleanedSegments);
	}

	private String normalizeName(String rawName) {
		if (!StringUtils.hasText(rawName)) {
			throw new InvalidFileRequestException("New name is required");
		}

		String name = rawName.trim();
		if (name.contains("/") || name.contains("\\")) {
			throw new InvalidFileRequestException("Name cannot contain path separators");
		}
		if (".".equals(name) || "..".equals(name)) {
			throw new InvalidFileRequestException("Name cannot be a relative path segment");
		}
		return name;
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String extractName(String normalizedPath) {
		int lastSeparator = normalizedPath.lastIndexOf('/');
		return lastSeparator >= 0 ? normalizedPath.substring(lastSeparator + 1) : normalizedPath;
	}

	private String parentPath(String normalizedPath) {
		int lastSeparator = normalizedPath.lastIndexOf('/');
		return lastSeparator >= 0 ? normalizedPath.substring(0, lastSeparator) : "";
	}

	private String joinPath(String parent, String child) {
		return StringUtils.hasText(parent) ? parent + "/" + child : child;
	}

	private long computeSize(String content) {
		return content.getBytes(StandardCharsets.UTF_8).length;
	}

	private CodeFile copyForProject(CodeFile sourceFile, Long targetProjectId, Long actorUserId) {
		CodeFile copiedFile = new CodeFile();
		copiedFile.setProjectId(targetProjectId);
		copiedFile.setName(sourceFile.getName());
		copiedFile.setPath(sourceFile.getPath());
		copiedFile.setLanguage(sourceFile.getLanguage());
		copiedFile.setContent(sourceFile.getContent());
		copiedFile.setSize(sourceFile.getSize());
		copiedFile.setCreatedById(actorUserId);
		copiedFile.setLastEditedBy(actorUserId);
		copiedFile.setDeleted(false);
		return copiedFile;
	}

	private void validatePositiveId(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new InvalidFileRequestException(fieldName + " must be greater than 0");
		}
	}

	private void sortTree(Iterable<FileTreeNode> nodes) {
		for (FileTreeNode node : nodes) {
			node.getChildren().sort(Comparator.comparing(FileTreeNode::isFolder).reversed()
					.thenComparing(FileTreeNode::getName, String.CASE_INSENSITIVE_ORDER));
			sortTree(node.getChildren());
		}
	}
}
