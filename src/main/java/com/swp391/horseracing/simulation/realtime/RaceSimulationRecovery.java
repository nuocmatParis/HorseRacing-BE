package com.swp391.horseracing.simulation.realtime;

import tools.jackson.databind.ObjectMapper;
import com.swp391.horseracing.simulation.domain.SimulationStatus;
import com.swp391.horseracing.simulation.engine.GeneratedSimulation;
import com.swp391.horseracing.simulation.persistence.RaceSimulationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RaceSimulationRecovery {
    private final RaceSimulationSessionRepository sessionRepository;
    private final RaceSimulationScheduler scheduler;
    private final RaceSimulationFrameService frameService;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void resumeRunningSessions() {
        sessionRepository.findByStatusOrderByStartedAtAsc(SimulationStatus.RUNNING).forEach(session -> {
            try {
                if (session.getTimelinePayload() == null) {
                    frameService.publishAborted(session.getSessionId(), "Stored timeline is missing after restart.");
                    return;
                }
                GeneratedSimulation simulation = objectMapper.readValue(
                        session.getTimelinePayload(), GeneratedSimulation.class);
                int nextFrame = session.getCurrentSnapshotJson() == null
                        ? 0 : Math.toIntExact(session.getCurrentSequence() + 1);
                scheduler.start(session.getSessionId(), simulation, nextFrame);
                log.info("Resumed live race scheduler sessionId={} from sequence={}",
                        session.getSessionId(), nextFrame);
            } catch (Exception exception) {
                log.error("Could not resume live race sessionId={}", session.getSessionId(), exception);
                frameService.publishAborted(session.getSessionId(), "The live session could not recover after restart.");
            }
        });
    }
}
