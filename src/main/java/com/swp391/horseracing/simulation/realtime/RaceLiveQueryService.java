package com.swp391.horseracing.simulation.realtime;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.simulation.api.*;
import com.swp391.horseracing.simulation.domain.SimulationRunnerStatus;
import com.swp391.horseracing.simulation.domain.SimulationStatus;
import com.swp391.horseracing.simulation.engine.GeneratedSimulation;
import com.swp391.horseracing.simulation.engine.SimulationFrame;
import com.swp391.horseracing.simulation.engine.TelemetryHorse;
import com.swp391.horseracing.simulation.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RaceLiveQueryService {
    private final RaceSimulationSessionRepository sessionRepository;
    private final RaceSimulationParticipantRepository participantRepository;
    private final RaceProvisionalResultRepository provisionalResultRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public LiveRaceSnapshotResponse snapshot(UUID raceId) {
        RaceSimulationSession session = sessionRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.SIMULATION_SESSION_NOT_FOUND));
        List<RaceSimulationParticipant> participants = participantRepository
                .findBySession_SessionIdOrderByLaneNumberAsc(session.getSessionId());
        GeneratedSimulation generated = readTimeline(session);
        double distance = generated == null
                ? session.getRace().getDistance().getMeters()
                : generated.raceDistanceMeters();
        int laps = generated == null
                ? Math.max(2, (int) Math.ceil(distance / 500.0))
                : generated.totalLaps();
        double lapLength = generated == null ? distance / laps : generated.lapLengthMeters();
        SimulationFrame frame = readCurrentFrame(session);
        List<TelemetryHorse> horses = frame == null
                ? participants.stream().map(participant -> readyHorse(participant, session.getStatus())).toList()
                : frame.horses();
        List<UUID> ranking = horses.stream()
                .sorted(Comparator.comparingInt(TelemetryHorse::rank))
                .map(TelemetryHorse::entryId)
                .toList();

        return new LiveRaceSnapshotResponse(
                raceId,
                session.getRace().getName(),
                session.getRace().getRound().getTournament().getName(),
                session.getSessionId(),
                session.getStatus(),
                session.getCurrentSequence(),
                session.getCurrentRaceTimeSeconds(),
                distance,
                lapLength,
                laps,
                participants.stream().map(this::participantResponse).toList(),
                horses,
                ranking,
                frame == null ? List.of() : frame.publicEvents(),
                session.getStatus() == SimulationStatus.FINISHED
                        ? provisionalResults(raceId, false) : List.of());
    }

    @Transactional(readOnly = true)
    public List<LiveRaceSummaryResponse> liveRaces() {
        List<RaceSimulationSession> sessions = new ArrayList<>(
                sessionRepository.findByStatusOrderByStartedAtAsc(SimulationStatus.RUNNING));
        if (sessions.isEmpty()) {
            sessionRepository.findFirstByStatusAndFinishedAtAfterOrderByFinishedAtDesc(
                    SimulationStatus.FINISHED, LocalDateTime.now().minusMinutes(30)).ifPresent(sessions::add);
        }
        return sessions.stream()
                .map(session -> {
                    SimulationFrame frame = readCurrentFrame(session);
                    TelemetryHorse leader = frame == null ? null : frame.horses().stream()
                            .min(Comparator.comparingInt(TelemetryHorse::rank)).orElse(null);
                    double distance = session.getRace().getDistance().getMeters();
                    double progress = leader == null ? 0.0 : Math.min(100.0, leader.distance() / distance * 100.0);
                    return new LiveRaceSummaryResponse(
                            session.getRace().getRaceId(),
                            session.getSessionId(),
                            session.getRace().getName(),
                            session.getRace().getRound().getTournament().getName(),
                            session.getCurrentRaceTimeSeconds(),
                            Math.round(progress * 10.0) / 10.0,
                            leader == null ? null : leader.horseName());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProvisionalResultResponse> provisionalResults(UUID raceId, boolean requireReferee) {
        if (requireReferee) {
            // Authorization is deliberately performed by the caller's referee-only service/controller.
        }
        return provisionalResultRepository.findByRace_RaceIdOrderByFinishPositionAsc(raceId).stream()
                .map(result -> new ProvisionalResultResponse(
                        result.getEntry().getEntryId(),
                        result.getEntry().getContract().getHorse().getHorseId(),
                        result.getEntry().getContract().getHorse().getName(),
                        result.getEntry().getContract().getJockey().getUser().getFullName(),
                        result.getEntry().getLaneNumber(),
                        result.getFinishPosition(),
                        result.getFinishTime(),
                        result.getStatus()))
                .toList();
    }

    private SimulationParticipantResponse participantResponse(RaceSimulationParticipant participant) {
        return new SimulationParticipantResponse(
                participant.getEntry().getEntryId(),
                participant.getHorseId(),
                participant.getHorseName(),
                participant.getHorseImageUrl(),
                participant.getJockeyId(),
                participant.getJockeyName(),
                participant.getLaneNumber());
    }

    private TelemetryHorse readyHorse(RaceSimulationParticipant participant, SimulationStatus sessionStatus) {
        SimulationRunnerStatus status = sessionStatus == SimulationStatus.READY
                ? SimulationRunnerStatus.READY : SimulationRunnerStatus.RUNNING;
        return new TelemetryHorse(
                participant.getEntry().getEntryId(),
                participant.getHorseId(),
                participant.getHorseName(),
                participant.getHorseImageUrl(),
                participant.getJockeyName(),
                participant.getLaneNumber(),
                1,
                0.0,
                0.0,
                100.0,
                participant.getLaneNumber(),
                status);
    }

    private GeneratedSimulation readTimeline(RaceSimulationSession session) {
        if (session.getTimelinePayload() == null) return null;
        try {
            return objectMapper.readValue(session.getTimelinePayload(), GeneratedSimulation.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored race simulation timeline is invalid", exception);
        }
    }

    private SimulationFrame readCurrentFrame(RaceSimulationSession session) {
        if (session.getCurrentSnapshotJson() == null) return null;
        try {
            return objectMapper.readValue(session.getCurrentSnapshotJson(), SimulationFrame.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Stored current race snapshot is invalid", exception);
        }
    }
}
