package com.codesync.comment.repository;

import com.codesync.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findByFileIdOrderByLineNumberAscColumnNumberAscCreatedAtAscCommentIdAsc(Long fileId);

	List<Comment> findByProjectIdOrderByCreatedAtDescCommentIdDesc(Long projectId);

	List<Comment> findByAuthorIdOrderByCreatedAtDescCommentIdDesc(Long authorId);

	List<Comment> findByFileIdAndLineNumberOrderByColumnNumberAscCreatedAtAscCommentIdAsc(Long fileId,
			Integer lineNumber);

	List<Comment> findByParentCommentIdOrderByCreatedAtAscCommentIdAsc(Long parentCommentId);

	List<Comment> findByResolvedOrderByCreatedAtDescCommentIdDesc(boolean resolved);

	List<Comment> findByProjectIdAndResolvedOrderByCreatedAtDescCommentIdDesc(Long projectId, boolean resolved);

	Optional<Comment> findByCommentId(Long commentId);

	long countByFileId(Long fileId);

	void deleteByCommentId(Long commentId);
}
