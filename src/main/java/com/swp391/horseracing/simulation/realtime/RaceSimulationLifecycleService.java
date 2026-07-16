package com.swp391.horseracing.simulation.realtime;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.swp391.horseracing.dto.race.response.RaceStartReadinessResponse;
import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.RaceEntryStatus;
import com.swp391.horseracing.enums.RoundStatus;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.repository.HorseInspectionRepository;
import com.swp391.horseracing.repository.RaceEntryRepository;
import com.swp391.horseracing.repository.RaceRepository;
import com.swp391.horseracing.service.RaceService;
import com.swp391.horseracing.simulation.api.LiveRaceSnapshotResponse;
import com.swp391.horseracing.simulation.api.LiveRaceMessage;
import com.swp391.horseracing.simulation.api.SimulationParticipantResponse;
import com.swp391.horseracing.simulation.domain.SimulationStatus;
import com.swp391.horseracing.simulation.engine.*;
import com.swp391.horseracing.simulation.mapper.SimulationProfileMapper;
import com.swp391.horseracing.simulation.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RaceSimulationLifecycleService {
    private final RaceService raceService;
    private final RaceRepository raceRepository;
    private final RaceEntryRepository raceEntryRepository;
    private final HorseInspectionRepository horseInspectionRepository;
    private final RaceSimulationSessionRepository sessionRepository;
    private final RaceSimulationParticipantRepository participantRepository;
    private final SimulationProfileMapper profileMapper;
    private final DeterministicRaceEngine raceEngine;
    private final SimulationAccessService accessService;
    private final ObjectMapper objectMapper;
    private final RaceControlPublisher publisher;

    @Value("${race.simulation.demo-seed:0}")
    private long configuredDemoSeed;

    @Value("${race.simulation.demo-mode:false}")
    private boolean demoMode;

    @Transactional
    public LiveRaceSnapshotResponse prepare(UUID raceId) {
        SimulationAccessService.AccessContext access = accessService.requireAssignedReferee(raceId);
        RaceStartReadinessResponse readiness = raceService.getStartReadiness(raceId);

        Race race = raceRepository.findForUpdateByRaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.RACE_NOT_FOUND));
        Optional<RaceSimulationSession> existing = sessionRepository.findForUpdateByRaceId(raceId);
        if (existing.isPresent()) {
            RaceSimulationSession session = existing.get();
            if (session.getStatus() == SimulationStatus.READY
                    || session.getStatus() == SimulationStatus.RUNNING
                    || session.getStatus() == SimulationStatus.FINISHED) {
                return preparedSnapshot(session);
            }
            throw new AppException(ErrorCode.SIMULATION_PREPARE_NOT_ALLOWED);
        }
        if (!readiness.isCanStart() || race.getStatus() != RoundStatus.SCHEDULED) {
            throw new AppException(ErrorCode.SIMULATION_PREPARE_NOT_ALLOWED);
        }

        long seed = configuredDemoSeed != 0
                ? configuredDemoSeed
                : raceId.getMostSignificantBits() ^ raceId.getLeastSignificantBits() ^ System.nanoTime();
        RaceSimulationSession session = sessionRepository.save(RaceSimulationSession.builder()
                .race(race)
                .status(SimulationStatus.PREPARING)
                .randomSeed(seed)
                .preparedAt(LocalDateTime.now())
                .preparedBy(access.user())
                .currentRaceTimeSeconds(0.0)
                .currentSequence(0L)
                .build());

        List<RaceEntry> entries = raceEntryRepository.findByRace_RaceIdOrderByLaneNumberAsc(raceId).stream()
                .filter(this::isActive)
                .toList();
        Set<Integer> lanes = new HashSet<>();
        for (RaceEntry entry : entries) {
            if (entry.getLaneNumber() == null || !lanes.add(entry.getLaneNumber())) {
                throw new AppException(ErrorCode.SIMULATION_PREPARE_NOT_ALLOWED);
            }
            HorseInspection inspection = horseInspectionRepository
                    .findByRaceEntry_EntryId(entry.getEntryId()).orElse(null);
            SimulationProfile profile = profileMapper.map(entry, inspection);
            participantRepository.save(toEntity(session, entry, profile));
        }
        session.setStatus(SimulationStatus.READY);
        sessionRepository.save(session);
        publisher.publishPrivate(race, new LiveRaceMessage(
                "SESSION_READY",
                raceId,
                session.getSessionId(),
                0L,
                0.0,
                LocalDateTime.now(),
                Map.of("participants", entries.size())));
        return preparedSnapshot(session);
    }

    @Transactional(readOnly = true)
    public void requireReady(UUID raceId) {
        accessService.requireAssignedReferee(raceId);
        RaceSimulationSession session = sessionRepository.findByRace_RaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.SIMULATION_SESSION_NOT_FOUND));
        if (session.getStatus() != SimulationStatus.READY) {
            throw new AppException(session.getStatus() == SimulationStatus.RUNNING
                    ? ErrorCode.SIMULATION_SESSION_ALREADY_STARTED
                    : ErrorCode.SIMULATION_SESSION_NOT_READY);
        }
    }

    @Transactional
    public RuntimePlan begin(UUID raceId) {
        SimulationAccessService.AccessContext access = accessService.requireAssignedReferee(raceId);
        RaceSimulationSession session = sessionRepository.findForUpdateByRaceId(raceId)
                .orElseThrow(() -> new AppException(ErrorCode.SIMULATION_SESSION_NOT_FOUND));
        if (session.getStatus() != SimulationStatus.READY) {
            throw new AppException(ErrorCode.SIMULATION_SESSION_NOT_READY);
        }
        if (session.getRace().getStatus() != RoundStatus.ONGOING) {
            throw new AppException(ErrorCode.INVALID_RACE_RESULT_STATUS);
        }

        List<SimulationProfile> profiles = participantRepository
                .findBySession_SessionIdOrderByLaneNumberAsc(session.getSessionId()).stream()
                .map(this::toProfile)
                .toList();
        double distance = session.getRace().getDistance().getMeters();
        GeneratedSimulation generated = raceEngine.generate(
                profiles,
                distance,
                trackFactor(session.getRace().getTrackCondition()),
                session.getRandomSeed(),
                demoMode);
        try {
            session.setTimelinePayload(objectMapper.writeValueAsString(generated));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize server-side simulation timeline", exception);
        }
        session.setStatus(SimulationStatus.RUNNING);
        session.setStartedAt(LocalDateTime.now());
        session.setStartedBy(access.user());
        session.setCurrentSequence(0L);
        session.setCurrentRaceTimeSeconds(0.0);
        sessionRepository.save(session);
        return new RuntimePlan(session.getSessionId(), generated);
    }

    @Transactional
    public void abort(UUID raceId) {
        sessionRepository.findForUpdateByRaceId(raceId).ifPresent(session -> {
            session.setStatus(SimulationStatus.ABORTED);
            session.setFinishedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    private LiveRaceSnapshotResponse preparedSnapshot(RaceSimulationSession session) {
        List<SimulationParticipantResponse> participants = participantRepository
                .findBySession_SessionIdOrderByLaneNumberAsc(session.getSessionId()).stream()
                .map(this::toResponse)
                .toList();
        double distance = session.getRace().getDistance().getMeters();
        int laps = Math.max(2, (int) Math.ceil(distance / 500.0));
        return new LiveRaceSnapshotResponse(
                session.getRace().getRaceId(),
                session.getRace().getName(),
                session.getRace().getRound().getTournament().getName(),
                session.getSessionId(),
                session.getStatus(),
                session.getCurrentSequence(),
                session.getCurrentRaceTimeSeconds(),
                distance,
                distance / laps,
                laps,
                participants,
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }

    private RaceSimulationParticipant toEntity(
            RaceSimulationSession session,
            RaceEntry entry,
            SimulationProfile profile) {
        return RaceSimulationParticipant.builder()
                .session(session)
                .entry(entry)
                .horseId(profile.horseId())
                .horseName(profile.horseName())
                .horseImageUrl(profile.horseImageUrl())
                .jockeyId(profile.jockeyId())
                .jockeyName(profile.jockeyName())
                .laneNumber(profile.laneNumber())
                .baseSpeed(profile.baseSpeed())
                .acceleration(profile.acceleration())
                .stamina(profile.stamina())
                .consistency(profile.consistency())
                .jockeySkill(profile.jockeySkill())
                .jockeyAggressiveness(profile.jockeyAggressiveness())
                .corneringSkill(profile.corneringSkill())
                .staminaManagement(profile.staminaManagement())
                .handicapWeight(profile.handicapWeight())
                .build();
    }

    private SimulationProfile toProfile(RaceSimulationParticipant participant) {
        return new SimulationProfile(
                participant.getEntry().getEntryId(),
                participant.getHorseId(),
                participant.getHorseName(),
                participant.getHorseImageUrl(),
                participant.getJockeyId(),
                participant.getJockeyName(),
                participant.getLaneNumber(),
                participant.getBaseSpeed(),
                participant.getAcceleration(),
                participant.getStamina(),
                participant.getConsistency(),
                participant.getJockeySkill(),
                participant.getJockeyAggressiveness(),
                participant.getCorneringSkill(),
                participant.getStaminaManagement(),
                participant.getHandicapWeight());
    }

    private SimulationParticipantResponse toResponse(RaceSimulationParticipant participant) {
        return new SimulationParticipantResponse(
                participant.getEntry().getEntryId(),
                participant.getHorseId(),
                participant.getHorseName(),
                participant.getHorseImageUrl(),
                participant.getJockeyId(),
                participant.getJockeyName(),
                participant.getLaneNumber());
    }

    private boolean isActive(RaceEntry entry) {
        return entry.getStatus() != RaceEntryStatus.SCRATCHED
                && entry.getStatus() != RaceEntryStatus.WITHDRAWN_BEFORE_SCHEDULE
                && entry.getStatus() != RaceEntryStatus.WITHDRAWN_AFTER_SCHEDULE
                && entry.getStatus() != RaceEntryStatus.DISQUALIFIED;
    }

    private double trackFactor(String condition) {
        String normalized = condition == null ? "" : condition.toUpperCase(Locale.ROOT);
        if (normalized.contains("MUD")) return 0.89;
        if (normalized.contains("WET") || normalized.contains("RAIN")) return 0.93;
        if (normalized.contains("SOFT")) return 0.96;
        return 1.0;
    }

    public record RuntimePlan(UUID sessionId, GeneratedSimulation generated) {
    }
}
