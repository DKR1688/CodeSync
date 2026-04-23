package com.codesync.comment.service;

import com.codesync.comment.entity.Comment;

import java.util.List;

public interface CommentService {

	Comment addComment(Comment comment, String authorizationHeader);

	List<Comment> getByFile(Long fileId);

	List<Comment> getByProject(Long projectId);

	Comment getCommentById(Long commentId);

	List<Comment> getReplies(Long commentId);

	Comment updateComment(Long commentId, String content);

	void deleteComment(Long commentId);

	Comment resolveComment(Long commentId);

	Comment unresolveComment(Long commentId);

	List<Comment> getByLine(Long fileId, Integer lineNumber);

	long getCommentCount(Long fileId);

	List<Comment> getByResolved(Long projectId, boolean resolved);
}
