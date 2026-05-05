package com.codesync.version.service;

import com.codesync.version.dto.FileUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VersionSnapshotListenerTest {

	@Mock
	private VersionServiceImpl versionService;

	@Test
	void handleFileUpdatedDelegatesToVersionService() {
		VersionSnapshotListener listener = new VersionSnapshotListener(versionService);
		FileUpdatedEvent event = new FileUpdatedEvent();
		event.setFileId(5L);

		listener.handleFileUpdated(event);

		verify(versionService).createSnapshotFromFileUpdate(event);
	}
}
