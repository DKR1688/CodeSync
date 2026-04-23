package com.codesync.collab.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BroadcastChangeRequest {

	@NotNull(message = "Base revision is required")
	@Min(value = 0, message = "Base revision cannot be negative")
	private Integer baseRevision;

	@NotNull(message = "Content is required")
	private String content;

	@Min(value = 1, message = "Cursor line must be at least 1")
	private Integer cursorLine;

	@Min(value = 1, message = "Cursor column must be at least 1")
	private Integer cursorCol;

	@Min(value = 1, message = "Selection end line must be at least 1")
	private Integer selectionEndLine;

	@Min(value = 1, message = "Selection end column must be at least 1")
	private Integer selectionEndCol;

	public Integer getBaseRevision() {
		return baseRevision;
	}

	public void setBaseRevision(Integer baseRevision) {
		this.baseRevision = baseRevision;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getCursorLine() {
		return cursorLine;
	}

	public void setCursorLine(Integer cursorLine) {
		this.cursorLine = cursorLine;
	}

	public Integer getCursorCol() {
		return cursorCol;
	}

	public void setCursorCol(Integer cursorCol) {
		this.cursorCol = cursorCol;
	}

	public Integer getSelectionEndLine() {
		return selectionEndLine;
	}

	public void setSelectionEndLine(Integer selectionEndLine) {
		this.selectionEndLine = selectionEndLine;
	}

	public Integer getSelectionEndCol() {
		return selectionEndCol;
	}

	public void setSelectionEndCol(Integer selectionEndCol) {
		this.selectionEndCol = selectionEndCol;
	}
}
