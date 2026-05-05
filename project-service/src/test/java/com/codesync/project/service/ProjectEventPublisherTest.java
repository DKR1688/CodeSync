package com.codesync.project.service;

import com.codesync.project.dto.NotificationCommand;
import com.codesync.project.dto.ProjectDTO;
import com.codesync.project.dto.ProjectEvent;
import com.codesync.project.dto.ProjectMemberEvent;
import com.codesync.project.enums.Visibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectEventPublisherTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Test
	void publishProjectCreatedMapsProjectDetails() {
		ProjectEventPublisher publisher = new ProjectEventPublisher(
				rabbitTemplate,
				"codesync.events",
				"notification.send",
				"project.created",
				"project.member.added");

		ProjectDTO project = new ProjectDTO();
		project.setProjectId(1L);
		project.setOwnerId(2L);
		project.setName("Compiler");
		project.setLanguage("Java");
		project.setVisibility(Visibility.PUBLIC);

		publisher.publishProjectCreated(project);

		ArgumentCaptor<ProjectEvent> eventCaptor = ArgumentCaptor.forClass(ProjectEvent.class);
		verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("codesync.events"),
				org.mockito.ArgumentMatchers.eq("project.created"), eventCaptor.capture());

		ProjectEvent event = eventCaptor.getValue();
		assertThat(event.getEventId()).isNotNull();
		assertThat(event.getOccurredAt()).isNotNull();
		assertThat(event.getProjectId()).isEqualTo(1L);
		assertThat(event.getOwnerId()).isEqualTo(2L);
		assertThat(event.getName()).isEqualTo("Compiler");
		assertThat(event.getLanguage()).isEqualTo("Java");
		assertThat(event.getVisibility()).isEqualTo("PUBLIC");
	}

	@Test
	void publishMemberAddedSendsMembershipEventAndNotification() {
		ProjectEventPublisher publisher = new ProjectEventPublisher(
				rabbitTemplate,
				"codesync.events",
				"notification.send",
				"project.created",
				"project.member.added");

		ProjectDTO project = new ProjectDTO();
		project.setProjectId(11L);
		project.setName("CodeSync");

		publisher.publishMemberAdded(project, 42L, 7L);

		InOrder inOrder = inOrder(rabbitTemplate);
		ArgumentCaptor<ProjectMemberEvent> memberEventCaptor = ArgumentCaptor.forClass(ProjectMemberEvent.class);
		ArgumentCaptor<NotificationCommand> notificationCaptor = ArgumentCaptor.forClass(NotificationCommand.class);

		inOrder.verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("codesync.events"),
				org.mockito.ArgumentMatchers.eq("project.member.added"), memberEventCaptor.capture());
		inOrder.verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("codesync.events"),
				org.mockito.ArgumentMatchers.eq("notification.send"), notificationCaptor.capture());

		ProjectMemberEvent memberEvent = memberEventCaptor.getValue();
		assertThat(memberEvent.getProjectId()).isEqualTo(11L);
		assertThat(memberEvent.getMemberUserId()).isEqualTo(42L);
		assertThat(memberEvent.getActorId()).isEqualTo(7L);
		assertThat(memberEvent.getProjectName()).isEqualTo("CodeSync");

		NotificationCommand notification = notificationCaptor.getValue();
		assertThat(notification.getRecipientId()).isEqualTo(42L);
		assertThat(notification.getActorId()).isEqualTo(7L);
		assertThat(notification.getType()).isEqualTo("SESSION_INVITE");
		assertThat(notification.getTitle()).isEqualTo("You were added to a project");
		assertThat(notification.getMessage()).contains("CodeSync");
		assertThat(notification.getRelatedId()).isEqualTo("11");
		assertThat(notification.getRelatedType()).isEqualTo("PROJECT");
		assertThat(notification.getDeepLinkUrl()).isEqualTo("/projects/11");
	}
}
