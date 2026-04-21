package com.codesync.collab.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CursorUpdateRequest {

	@NotNull(message = "Cursor line is required")
	@Min(value = 1, message = "Cursor line must be at least 1")
	private Integer cursorLine;

	@NotNull(message = "Cursor column is required")
	@Min(value = 1, message = "Cursor column must be at least 1")
	private Integer cursorCol;

	@Min(value = 1, message = "Selection end line must be at least 1")
	private Integer selectionEndLine;

	@Min(value = 1, message = "Selection end column must be at least 1")
	private Integer selectionEndCol;

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
