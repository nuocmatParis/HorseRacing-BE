package com.swp391.horseracing.simulation.realtime;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.simulation.api.*;
import com.swp391.horseracing.simulation.domain.SimulationStatus;
import com.swp391.horseracing.simulation.domain.WarningReviewStatus;
import com.swp391.horseracing.simulation.engine.*;
import com.swp391.horseracing.simulation.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RaceSimulationFrameService {
    private final RaceSimulationSessionRepository sessionRepository;
    private final RaceSimulationParticipantRepository participantRepository;
    private final RaceSimulationWarningRepository warningRepository;
    private final RaceProvisionalResultRepository provisionalResultRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final RaceControlPublisher publisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(UUID sessionId, GeneratedSimulation simulation, SimulationFrame frame, boolean last) {
        RaceSimulationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SIMULATION_SESSION_NOT_FOUND));
        if (session.getStatus() != SimulationStatus.RUNNING) return;

        session.setCurrentSequence(frame.sequence());
        session.setCurrentRaceTimeSeconds(frame.raceTimeSeconds());
        try {
            session.setCurrentSnapshotJson(objectMapper.writeValueAsString(frame));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize current race frame", exception);
        }
        sessionRepository.save(session);

        Map<UUID, RaceEntry> entries = new HashMap<>();
        for (RaceSimulationParticipant participant : participantRepository
                .findBySession_SessionIdOrderByLaneNumberAsc(sessionId)) {
            entries.put(participant.getEntry().getEntryId(), participant.getEntry());
        }
        List<SimulationWarningResponse> newWarnings = persistWarnings(session, frame, entries);

        if (frame.sequence() == 0) {
            LiveRaceMessage started = message("RACE_STARTED", session, frame,
                    Map.of("message", "The race has started."));
            publisher.publishPublic(session.getRace().getRaceId(), started);
            publisher.publishPrivate(session.getRace(), started);
        }

        Map<String, Object> publicPayload = new LinkedHashMap<>();
        publicPayload.put("raceDistanceMeters", simulation.raceDistanceMeters());
        publicPayload.put("lapLengthMeters", simulation.lapLengthMeters());
        publicPayload.put("totalLaps", simulation.totalLaps());
        publicPayload.put("horses", frame.horses());
        publicPayload.put("ranking", frame.horses().stream()
                .sorted(Comparator.comparingInt(TelemetryHorse::rank))
                .map(TelemetryHorse::entryId).toList());
        publicPayload.put("publicEvents", frame.publicEvents());
        LiveRaceMessage telemetry = message("TELEMETRY_FRAME", session, frame, publicPayload);
        publisher.publishPublic(session.getRace().getRaceId(), telemetry);
        publisher.publishPublic(session.getRace().getRaceId(), message(
                "RANKING_UPDATED",
                session,
                frame,
                Map.of("ranking", publicPayload.get("ranking"))));
        for (String event : frame.publicEvents()) {
            publisher.publishPublic(session.getRace().getRaceId(), message(
                    "RACE_EVENT", session, frame, Map.of("message", event)));
        }

        for (SimulationWarningResponse warning : newWarnings) {
            publisher.publishPrivate(session.getRace(), message("SYSTEM_WARNING", session, frame, warning));
        }

        if (last) {
            persistProvisionalResults(session, simulation.provisionalResults(), entries);
            session.setStatus(SimulationStatus.FINISHED);
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);
            Map<String, Object> finishPayload = Map.of(
                    "provisionalResults", simulation.provisionalResults(),
                    "message", "Provisional result - awaiting referee confirmation");
            LiveRaceMessage finish = message("RACE_FINISHED", session, frame, finishPayload);
            publisher.publishPublic(session.getRace().getRaceId(), finish);
            publisher.publishPrivate(session.getRace(), finish);
        }
    }

    @Transactional
    public void publishAborted(UUID sessionId, String reason) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setStatus(SimulationStatus.ABORTED);
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);
            LiveRaceMessage message = new LiveRaceMessage(
                    "SESSION_ABORTED",
                    session.getRace().getRaceId(),
                    sessionId,
                    session.getCurrentSequence(),
                    session.getCurrentRaceTimeSeconds(),
                    LocalDateTime.now(),
                    Map.of("message", reason));
            publisher.publishPublic(session.getRace().getRaceId(), message);
            publisher.publishPrivate(session.getRace(), message);
        });
    }

    private List<SimulationWarningResponse> persistWarnings(
            RaceSimulationSession session,
            SimulationFrame frame,
            Map<UUID, RaceEntry> entries) {
        List<SimulationWarningResponse> responses = new ArrayList<>();
        for (DetectedWarning detected : frame.warnings()) {
            if (warningRepository.existsBySession_SessionIdAndEntry_EntryIdAndWarningTypeAndSequence(
                    session.getSessionId(), detected.entryId(), detected.type(), frame.sequence())) {
                continue;
            }
            RaceEntry entry = entries.get(detected.entryId());
            if (entry == null) continue;
            RaceSimulationWarning warning = warningRepository.save(RaceSimulationWarning.builder()
                    .session(session)
                    .race(session.getRace())
                    .entry(entry)
                    .horseId(detected.horseId())
                    .warningType(detected.type())
                    .severity(detected.severity())
                    .riskScore(detected.riskScore())
                    .sequence(frame.sequence())
                    .raceTimeSeconds(frame.raceTimeSeconds())
                    .message(detected.message())
                    .suggestedAction(detected.suggestedAction())
                    .createdAt(LocalDateTime.now())
                    .reviewStatus(WarningReviewStatus.PENDING)
                    .build());
            responses.add(toResponse(warning));
        }
        return responses;
    }

    private void persistProvisionalResults(
            RaceSimulationSession session,
            List<ProvisionalStanding> standings,
            Map<UUID, RaceEntry> entries) {
        if (provisionalResultRepository.existsBySession_SessionId(session.getSessionId())) return;
        for (ProvisionalStanding standing : standings) {
            RaceEntry entry = entries.get(standing.entryId());
            if (entry == null) continue;
            provisionalResultRepository.save(RaceProvisionalResult.builder()
                    .session(session)
                    .race(session.getRace())
                    .entry(entry)
                    .finishPosition(standing.finishPosition())
                    .finishTime(standing.finishTime())
                    .status(standing.status())
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    private LiveRaceMessage message(String type, RaceSimulationSession session, SimulationFrame frame, Object payload) {
        return new LiveRaceMessage(
                type,
                session.getRace().getRaceId(),
                session.getSessionId(),
                frame.sequence(),
                frame.raceTimeSeconds(),
                LocalDateTime.now(),
                payload);
    }

    private SimulationWarningResponse toResponse(RaceSimulationWarning warning) {
        return new SimulationWarningResponse(
                warning.getWarningId(),
                warning.getEntry().getEntryId(),
                warning.getHorseId(),
                warning.getEntry().getContract().getHorse().getName(),
                warning.getWarningType(),
                warning.getSeverity(),
                warning.getRiskScore(),
                warning.getRaceTimeSeconds(),
                warning.getMessage(),
                warning.getSuggestedAction(),
                warning.getReviewStatus(),
                warning.getCreatedAt(),
                warning.getReviewedAt(),
                warning.getReviewNote());
    }
}
