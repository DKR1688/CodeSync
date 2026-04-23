package com.codesync.comment.service;

import com.codesync.comment.client.AuthUserClient;
import com.codesync.comment.client.NotificationClient;
import com.codesync.comment.dto.NotificationRequest;
import com.codesync.comment.entity.Comment;
import com.codesync.comment.exception.InvalidCommentRequestException;
import com.codesync.comment.exception.ResourceNotFoundException;
import com.codesync.comment.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

	private static final int MAX_CONTENT_LENGTH = 10_000;
	private static final Pattern MENTION_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9._-]{1,50})");

	private final CommentRepository repository;
	private final AuthUserClient authUserClient;
	private final NotificationClient notificationClient;
	private final boolean notificationsEnabled;

	public CommentServiceImpl(CommentRepository repository, AuthUserClient authUserClient,
			NotificationClient notificationClient,
			@Value("${comment.mentions.notifications-enabled:true}") boolean notificationsEnabled) {
		this.repository = repository;
		this.authUserClient = authUserClient;
		this.notificationClient = notificationClient;
		this.notificationsEnabled = notificationsEnabled;
	}

	@Override
	public Comment addComment(Comment comment, String authorizationHeader) {
		if (comment == null) {
			throw new InvalidCommentRequestException("Comment payload is required");
		}

		validatePositiveId(comment.getProjectId(), "Project id");
		validatePositiveId(comment.getFileId(), "File id");
		validatePositiveId(comment.getAuthorId(), "Author id");
		validateNullablePositiveId(comment.getSnapshotId(), "Snapshot id");

		Comment savedComment = new Comment();
		savedComment.setProjectId(comment.getProjectId());
		savedComment.setFileId(comment.getFileId());
		savedComment.setAuthorId(comment.getAuthorId());
		savedComment.setContent(normalizeContent(comment.getContent()));
		savedComment.setResolved(false);

		if (comment.getParentCommentId() != null) {
			applyReplyAnchor(comment, savedComment);
		} else {
			validatePositiveInteger(comment.getLineNumber(), "Line number");
			validateNullablePositiveInteger(comment.getColumnNumber(), "Column number");
			savedComment.setLineNumber(comment.getLineNumber());
			savedComment.setColumnNumber(comment.getColumnNumber() != null ? comment.getColumnNumber() : 1);
			savedComment.setSnapshotId(comment.getSnapshotId());
		}

		Comment persisted = repository.save(savedComment);
		dispatchMentionNotifications(persisted, authorizationHeader);
		return persisted;
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> getByFile(Long fileId) {
		validatePositiveId(fileId, "File id");
		return repository.findByFileIdOrderByLineNumberAscColumnNumberAscCreatedAtAscCommentIdAsc(fileId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> getByProject(Long projectId) {
		validatePositiveId(projectId, "Project id");
		return repository.findByProjectIdOrderByCreatedAtDescCommentIdDesc(projectId);
	}

	@Override
	@Transactional(readOnly = true)
	public Comment getCommentById(Long commentId) {
		validatePositiveId(commentId, "Comment id");
		return getCommentOrThrow(commentId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> getReplies(Long commentId) {
		validatePositiveId(commentId, "Comment id");
		Comment parent = getCommentOrThrow(commentId);
		if (parent.getParentCommentId() != null) {
			return List.of();
		}
		return repository.findByParentCommentIdOrderByCreatedAtAscCommentIdAsc(commentId);
	}

	@Override
	public Comment updateComment(Long commentId, String content) {
		Comment comment = getCommentById(commentId);
		comment.setContent(normalizeContent(content));
		return repository.save(comment);
	}

	@Override
	public void deleteComment(Long commentId) {
		Comment comment = getCommentById(commentId);
		if (comment.getParentCommentId() == null) {
			repository.deleteAll(repository.findByParentCommentIdOrderByCreatedAtAscCommentIdAsc(commentId));
		}
		repository.deleteByCommentId(commentId);
	}

	@Override
	public Comment resolveComment(Long commentId) {
		Comment comment = getCommentById(commentId);
		comment.setResolved(true);
		return repository.save(comment);
	}

	@Override
	public Comment unresolveComment(Long commentId) {
		Comment comment = getCommentById(commentId);
		comment.setResolved(false);
		return repository.save(comment);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> getByLine(Long fileId, Integer lineNumber) {
		validatePositiveId(fileId, "File id");
		validatePositiveInteger(lineNumber, "Line number");
		return repository.findByFileIdAndLineNumberOrderByColumnNumberAscCreatedAtAscCommentIdAsc(fileId, lineNumber);
	}

	@Override
	@Transactional(readOnly = true)
	public long getCommentCount(Long fileId) {
		validatePositiveId(fileId, "File id");
		return repository.countByFileId(fileId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Comment> getByResolved(Long projectId, boolean resolved) {
		if (projectId == null) {
			return repository.findByResolvedOrderByCreatedAtDescCommentIdDesc(resolved);
		}
		validatePositiveId(projectId, "Project id");
		return repository.findByProjectIdAndResolvedOrderByCreatedAtDescCommentIdDesc(projectId, resolved);
	}

	private void applyReplyAnchor(Comment request, Comment reply) {
		validatePositiveId(request.getParentCommentId(), "Parent comment id");
		Comment parent = getCommentOrThrow(request.getParentCommentId());
		if (parent.getParentCommentId() != null) {
			throw new InvalidCommentRequestException("Replies can only be added to top-level comments");
		}
		if (!parent.getProjectId().equals(request.getProjectId()) || !parent.getFileId().equals(request.getFileId())) {
			throw new InvalidCommentRequestException("Reply must belong to the same project and file as its parent");
		}
		if (request.getSnapshotId() != null && parent.getSnapshotId() != null
				&& !request.getSnapshotId().equals(parent.getSnapshotId())) {
			throw new InvalidCommentRequestException("Reply snapshot must match the parent comment snapshot");
		}

		validateNullablePositiveInteger(request.getLineNumber(), "Line number");
		validateNullablePositiveInteger(request.getColumnNumber(), "Column number");
		reply.setParentCommentId(parent.getCommentId());
		reply.setLineNumber(request.getLineNumber() != null ? request.getLineNumber() : parent.getLineNumber());
		reply.setColumnNumber(request.getColumnNumber() != null ? request.getColumnNumber() : parent.getColumnNumber());
		reply.setSnapshotId(request.getSnapshotId() != null ? request.getSnapshotId() : parent.getSnapshotId());
	}

	private void dispatchMentionNotifications(Comment comment, String authorizationHeader) {
		if (!notificationsEnabled) {
			return;
		}
		for (String username : extractMentionedUsernames(comment.getContent())) {
			authUserClient.findActiveUserByUsername(username, authorizationHeader)
					.filter(user -> !comment.getAuthorId().equals(user.getUserId()))
					.ifPresent(user -> notificationClient.send(buildMentionNotification(comment, user.getUserId()),
							authorizationHeader));
		}
	}

	private NotificationRequest buildMentionNotification(Comment comment, Long recipientId) {
		String relatedId = String.valueOf(comment.getCommentId());
		String deepLink = "/projects/" + comment.getProjectId()
				+ "/files/" + comment.getFileId()
				+ "?commentId=" + comment.getCommentId();
		return new NotificationRequest(recipientId, comment.getAuthorId(), "MENTION",
				"You were mentioned in a code comment",
				comment.getContent(),
				relatedId,
				"COMMENT",
				deepLink);
	}

	private Set<String> extractMentionedUsernames(String content) {
		Set<String> usernames = new LinkedHashSet<>();
		if (!StringUtils.hasText(content)) {
			return usernames;
		}

		Matcher matcher = MENTION_PATTERN.matcher(content);
		while (matcher.find()) {
			usernames.add(matcher.group(1));
		}
		return usernames;
	}

	private Comment getCommentOrThrow(Long commentId) {
		return repository.findByCommentId(commentId)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found with id " + commentId));
	}

	private String normalizeContent(String content) {
		if (!StringUtils.hasText(content)) {
			throw new InvalidCommentRequestException("Comment content is required");
		}
		String normalized = content.trim();
		if (normalized.length() > MAX_CONTENT_LENGTH) {
			throw new InvalidCommentRequestException("Comment content must be 10000 characters or fewer");
		}
		return normalized;
	}

	private void validateNullablePositiveId(Long value, String fieldName) {
		if (value != null) {
			validatePositiveId(value, fieldName);
		}
	}

	private void validatePositiveId(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new InvalidCommentRequestException(fieldName + " must be greater than 0");
		}
	}

	private void validateNullablePositiveInteger(Integer value, String fieldName) {
		if (value != null) {
			validatePositiveInteger(value, fieldName);
		}
	}

	private void validatePositiveInteger(Integer value, String fieldName) {
		if (value == null || value <= 0) {
			throw new InvalidCommentRequestException(fieldName + " must be greater than 0");
		}
	}
}
