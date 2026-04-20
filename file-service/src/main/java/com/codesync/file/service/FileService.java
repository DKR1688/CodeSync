package com.codesync.file.service;

import com.codesync.file.dto.FileTreeNode;
import com.codesync.file.entity.CodeFile;

import java.util.List;

public interface FileService {

	CodeFile createFile(CodeFile file);

	CodeFile getFileById(Long fileId);

	List<CodeFile> getFilesByProject(Long projectId);

	String getFileContent(Long fileId);

	CodeFile updateFileContent(Long fileId, String content, Long editorId);

	CodeFile renameFile(Long fileId, String newName);

	void deleteFile(Long fileId);

	void restoreFile(Long fileId);

	CodeFile moveFile(Long fileId, String newPath);

	CodeFile createFolder(Long projectId, String folderPath, Long creatorId);

	List<FileTreeNode> getFileTree(Long projectId);

	List<CodeFile> searchInProject(Long projectId, String query);
}
