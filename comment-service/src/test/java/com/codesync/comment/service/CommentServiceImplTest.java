package com.codesync.comment.service;

import com.codesync.comment.client.AuthUserClient;
import com.codesync.comment.client.NotificationClient;
import com.codesync.comment.entity.Comment;
import com.codesync.comment.exception.InvalidCommentRequestException;
import com.codesync.comment.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CommentServiceImplTest {

	@Autowired
	private CommentService commentService;

	@Autowired
	private CommentRepository repository;

	@MockitoBean
	private AuthUserClient authUserClient;

	@MockitoBean
	private NotificationClient notificationClient;

	@BeforeEach
	void cleanRepository() {
		repository.deleteAll();
	}

	@Test
	void addCommentStoresInlineSnapshotAnchoredComment() {
		Comment saved = commentService.addComment(comment(null, " Please review this ", 12, null, 500L), "Bearer token");

		assertThat(saved.getCommentId()).isNotNull();
		assertThat(saved.getContent()).isEqualTo("Please review this");
		assertThat(saved.getLineNumber()).isEqualTo(12);
		assertThat(saved.getColumnNumber()).isEqualTo(1);
		assertThat(saved.getSnapshotId()).isEqualTo(500L);
		assertThat(saved.isResolved()).isFalse();
	}

	@Test
	void replyInheritsParentAnchorAndRejectsNestedReplies() {
		Comment parent = commentService.addComment(comment(null, "parent", 4, 3, 10L), null);
		Comment reply = commentService.addComment(comment(parent.getCommentId(), "reply", null, null, null), null);

		assertThat(reply.getParentCommentId()).isEqualTo(parent.getCommentId());
		assertThat(reply.getLineNumber()).isEqualTo(4);
		assertThat(reply.getColumnNumber()).isEqualTo(3);
		assertThat(reply.getSnapshotId()).isEqualTo(10L);

		assertThatThrownBy(() -> commentService.addComment(comment(reply.getCommentId(), "nested", null, null, null), null))
				.isInstanceOf(InvalidCommentRequestException.class);
	}

	@Test
	void resolveAndUnresolveTrackReviewWorkflow() {
		Comment saved = commentService.addComment(comment(null, "fix this", 8, null, null), null);

		assertThat(commentService.resolveComment(saved.getCommentId()).isResolved()).isTrue();
		assertThat(commentService.unresolveComment(saved.getCommentId()).isResolved()).isFalse();
	}

	@Test
	void deleteTopLevelCommentDeletesReplies() {
		Comment parent = commentService.addComment(comment(null, "parent", 2, null, null), null);
		commentService.addComment(comment(parent.getCommentId(), "reply", null, null, null), null);

		commentService.deleteComment(parent.getCommentId());

		assertThat(repository.findAll()).isEmpty();
	}

	@Test
	void getByLineAndCountReturnExpectedComments() {
		commentService.addComment(comment(null, "first", 7, 1, null), null);
		commentService.addComment(comment(null, "second", 7, 5, null), null);
		commentService.addComment(comment(null, "other", 8, 1, null), null);

		assertThat(commentService.getByLine(20L, 7)).extracting(Comment::getContent)
				.containsExactly("first", "second");
		assertThat(commentService.getCommentCount(20L)).isEqualTo(3);
	}

	private Comment comment(Long parentCommentId, String content, Integer lineNumber, Integer columnNumber,
			Long snapshotId) {
		Comment comment = new Comment();
		comment.setProjectId(10L);
		comment.setFileId(20L);
		comment.setAuthorId(99L);
		comment.setContent(content);
		comment.setLineNumber(lineNumber);
		comment.setColumnNumber(columnNumber);
		comment.setParentCommentId(parentCommentId);
		comment.setSnapshotId(snapshotId);
		return comment;
	}
}
