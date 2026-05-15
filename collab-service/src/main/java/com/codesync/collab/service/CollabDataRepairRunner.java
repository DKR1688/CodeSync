package com.codesync.collab.service;

import com.codesync.collab.entity.CollabSession;
import com.codesync.collab.entity.Participant;
import com.codesync.collab.repository.CollabSessionRepository;
import com.codesync.collab.repository.ParticipantRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
public class CollabDataRepairRunner implements ApplicationRunner {

	private final CollabSessionRepository sessionRepository;
	private final ParticipantRepository participantRepository;

	public CollabDataRepairRunner(CollabSessionRepository sessionRepository,
			ParticipantRepository participantRepository) {
		this.sessionRepository = sessionRepository;
		this.participantRepository = participantRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		List<CollabSession> sessionsToRepair = new ArrayList<>(sessionRepository.findAll());
		for (CollabSession session : sessionsToRepair) {
			session.applyLegacyDefaults();
		}
		if (!sessionsToRepair.isEmpty()) {
			sessionRepository.saveAll(sessionsToRepair);
		}

		List<Participant> participantsToRepair = new ArrayList<>(participantRepository.findAll());
		for (Participant participant : participantsToRepair) {
			participant.applyLegacyDefaults();
		}
		if (!participantsToRepair.isEmpty()) {
			participantRepository.saveAll(participantsToRepair);
		}
	}
}
