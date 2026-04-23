package com.codesync.version.dto;

import java.util.List;

public class DiffResponse {

	private Long fromSnapshotId;
	private Long toSnapshotId;
	private String fromHash;
	private String toHash;
	private int addedLines;
	private int removedLines;
	private List<DiffLine> lines;

	public DiffResponse() {
	}

	public DiffResponse(Long fromSnapshotId, Long toSnapshotId, String fromHash, String toHash,
			int addedLines, int removedLines, List<DiffLine> lines) {
		this.fromSnapshotId = fromSnapshotId;
		this.toSnapshotId = toSnapshotId;
		this.fromHash = fromHash;
		this.toHash = toHash;
		this.addedLines = addedLines;
		this.removedLines = removedLines;
		this.lines = lines;
	}

	public Long getFromSnapshotId() {
		return fromSnapshotId;
	}

	public void setFromSnapshotId(Long fromSnapshotId) {
		this.fromSnapshotId = fromSnapshotId;
	}

	public Long getToSnapshotId() {
		return toSnapshotId;
	}

	public void setToSnapshotId(Long toSnapshotId) {
		this.toSnapshotId = toSnapshotId;
	}

	public String getFromHash() {
		return fromHash;
	}

	public void setFromHash(String fromHash) {
		this.fromHash = fromHash;
	}

	public String getToHash() {
		return toHash;
	}

	public void setToHash(String toHash) {
		this.toHash = toHash;
	}

	public int getAddedLines() {
		return addedLines;
	}

	public void setAddedLines(int addedLines) {
		this.addedLines = addedLines;
	}

	public int getRemovedLines() {
		return removedLines;
	}

	public void setRemovedLines(int removedLines) {
		this.removedLines = removedLines;
	}

	public List<DiffLine> getLines() {
		return lines;
	}

	public void setLines(List<DiffLine> lines) {
		this.lines = lines;
	}
}
