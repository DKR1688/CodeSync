package com.codesync.file.service;

import com.codesync.file.dto.FileTreeNode;
import com.codesync.file.entity.CodeFile;
import com.codesync.file.exception.InvalidFileRequestException;
import com.codesync.file.exception.ResourceNotFoundException;
import com.codesync.file.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

	@Mock
	private FileRepository repository;

	@InjectMocks
	private FileServiceImpl service;

	private CodeFile file;

	@BeforeEach
	void setUp() {
		file = createFile(1L, 100L, "src/main/App.java", "Java");
	}

	@Test
	void createFileNormalizesPathAndInitializesManagedFields() {
		CodeFile request = new CodeFile();
		request.setProjectId(100L);
		request.setPath("\\src\\main\\App.java\\");
		request.setLanguage(" Java ");
		request.setContent("class App {}");
		request.setCreatedById(77L);
		when(repository.findByProjectIdAndPathAndIsDeletedFalse(100L, "src/main/App.java")).thenReturn(Optional.empty());
		when(repository.save(any(CodeFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CodeFile created = service.createFile(request);

		assertEquals("src/main/App.java", created.getPath());
		assertEquals("App.java", created.getName());
		assertEquals("Java", created.getLanguage());
		assertEquals(12, created.getSize());
		assertEquals(77L, created.getCreatedById());
		assertEquals(77L, created.getLastEditedBy());
		assertFalse(created.isDeleted());
	}

	@Test
	void createFileRejectsMissingLanguage() {
		CodeFile request = new CodeFile();
		request.setProjectId(100L);
		request.setPath("src/main/App.java");
		request.setCreatedById(77L);
		when(repository.findByProjectIdAndPathAndIsDeletedFalse(100L, "src/main/App.java")).thenReturn(Optional.empty());

		assertThrows(InvalidFileRequestException.class, () -> service.createFile(request));
		verify(repository, never()).save(any(CodeFile.class));
	}

	@Test
	void getFileByIdThrowsWhenMissing() {
		when(repository.findByFileId(999L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.getFileById(999L));
	}

	@Test
	void updateFileContentTracksEditorAndSize() {
		when(repository.findByFileId(1L)).thenReturn(Optional.of(file));
		when(repository.save(any(CodeFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CodeFile updated = service.updateFileContent(1L, "System.out.println(\"hi\");", 25L);

		assertEquals("System.out.println(\"hi\");", updated.getContent());
		assertEquals(25L, updated.getLastEditedBy());
		assertEquals(25, updated.getSize());
	}

	@Test
	void renameFolderUpdatesDescendantPaths() {
		CodeFile folder = createFolder(5L, 100L, "src/main");
		CodeFile child = createFile(6L, 100L, "src/main/App.java", "Java");
		when(repository.findByFileId(5L)).thenReturn(Optional.of(folder));
		when(repository.findByProjectIdOrderByPathAsc(100L)).thenReturn(List.of(folder, child));

		CodeFile renamed = service.renameFile(5L, "core");

		assertEquals("src/core", renamed.getPath());
		assertEquals("core", renamed.getName());
		assertEquals("src/core/App.java", child.getPath());
		verify(repository).saveAll(List.of(folder, child));
	}

	@Test
	void moveFolderUpdatesNestedPaths() {
		CodeFile folder = createFolder(5L, 100L, "src/main");
		CodeFile child = createFile(6L, 100L, "src/main/App.java", "Java");
		when(repository.findByFileId(5L)).thenReturn(Optional.of(folder));
		when(repository.findByProjectIdOrderByPathAsc(100L)).thenReturn(List.of(folder, child));

		CodeFile moved = service.moveFile(5L, "modules/main");

		assertEquals("modules/main", moved.getPath());
		assertEquals("main", moved.getName());
		assertEquals("modules/main/App.java", child.getPath());
		verify(repository).saveAll(List.of(folder, child));
	}

	@Test
	void deleteFileSoftDeletesFolderAndDescendants() {
		CodeFile folder = createFolder(5L, 100L, "src/main");
		CodeFile child = createFile(6L, 100L, "src/main/App.java", "Java");
		when(repository.findByFileId(5L)).thenReturn(Optional.of(folder));
		when(repository.findByProjectIdOrderByPathAsc(100L)).thenReturn(List.of(folder, child));

		service.deleteFile(5L);

		assertTrue(folder.isDeleted());
		assertTrue(child.isDeleted());
		verify(repository).saveAll(List.of(folder, child));
	}

	@Test
	void restoreFileRejectsActivePathConflicts() {
		CodeFile deletedFile = createFile(5L, 100L, "src/main/App.java", "Java");
		deletedFile.setDeleted(true);
		CodeFile activeFile = createFile(6L, 100L, "src/main/App.java", "Java");
		when(repository.findByFileId(5L)).thenReturn(Optional.of(deletedFile));
		when(repository.findByProjectIdOrderByPathAsc(100L)).thenReturn(List.of(deletedFile, activeFile));

		assertThrows(InvalidFileRequestException.class, () -> service.restoreFile(5L));
		verify(repository, never()).saveAll(any());
	}

	@Test
	void getFileTreeBuildsImplicitFolders() {
		when(repository.findByProjectIdAndIsDeletedFalseOrderByPathAsc(100L)).thenReturn(List.of(file));

		List<FileTreeNode> tree = service.getFileTree(100L);

		assertEquals(1, tree.size());
		assertEquals("src", tree.getFirst().getName());
		assertTrue(tree.getFirst().isFolder());
		assertEquals("main", tree.getFirst().getChildren().getFirst().getName());
		assertEquals("App.java", tree.getFirst().getChildren().getFirst().getChildren().getFirst().getName());
	}

	@Test
	void searchInProjectMatchesPathAndContent() {
		CodeFile notes = createFile(2L, 100L, "docs/notes.txt", "Text");
		notes.setContent("release checklist");
		when(repository.findByProjectIdAndIsDeletedFalseOrderByPathAsc(100L)).thenReturn(List.of(file, notes));

		List<CodeFile> result = service.searchInProject(100L, "check");

		assertEquals(1, result.size());
		assertEquals("docs/notes.txt", result.getFirst().getPath());
	}

	private CodeFile createFile(Long fileId, Long projectId, String path, String language) {
		CodeFile codeFile = new CodeFile();
		codeFile.setFileId(fileId);
		codeFile.setProjectId(projectId);
		codeFile.setPath(path);
		codeFile.setName(path.substring(path.lastIndexOf('/') + 1));
		codeFile.setLanguage(language);
		codeFile.setContent("");
		codeFile.setSize(0);
		codeFile.setCreatedById(10L);
		codeFile.setLastEditedBy(10L);
		return codeFile;
	}

	private CodeFile createFolder(Long fileId, Long projectId, String path) {
		CodeFile folder = createFile(fileId, projectId, path, null);
		folder.setContent("");
		return folder;
	}
}
