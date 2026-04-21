package com.codesync.version.dto;

public class DiffLine {

	private DiffOperation operation;
	private Integer oldLineNumber;
	private Integer newLineNumber;
	private String content;

	public DiffLine() {
	}

	public DiffLine(DiffOperation operation, Integer oldLineNumber, Integer newLineNumber, String content) {
		this.operation = operation;
		this.oldLineNumber = oldLineNumber;
		this.newLineNumber = newLineNumber;
		this.content = content;
	}

	public DiffOperation getOperation() {
		return operation;
	}

	public void setOperation(DiffOperation operation) {
		this.operation = operation;
	}

	public Integer getOldLineNumber() {
		return oldLineNumber;
	}

	public void setOldLineNumber(Integer oldLineNumber) {
		this.oldLineNumber = oldLineNumber;
	}

	public Integer getNewLineNumber() {
		return newLineNumber;
	}

	public void setNewLineNumber(Integer newLineNumber) {
		this.newLineNumber = newLineNumber;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
