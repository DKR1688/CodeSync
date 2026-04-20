package com.codesync.file.dto;

import java.util.ArrayList;
import java.util.List;

public class FileTreeNode {

	private Long fileId;
	private String name;
	private String path;
	private boolean folder;
	private String language;
	private final List<FileTreeNode> children = new ArrayList<>();

	public FileTreeNode() {
	}

	public FileTreeNode(Long fileId, String name, String path, boolean folder, String language) {
		this.fileId = fileId;
		this.name = name;
		this.path = path;
		this.folder = folder;
		this.language = language;
	}

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long fileId) {
		this.fileId = fileId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isFolder() {
		return folder;
	}

	public void setFolder(boolean folder) {
		this.folder = folder;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public List<FileTreeNode> getChildren() {
		return children;
	}
}
