package com.swp391.horseracing.simulation.api;

import com.swp391.horseracing.simulation.domain.SimulationStatus;
import com.swp391.horseracing.simulation.engine.TelemetryHorse;

import java.util.List;
import java.util.UUID;

public record LiveRaceSnapshotResponse(
        UUID raceId,
        String raceName,
        String tournamentName,
        UUID sessionId,
        SimulationStatus status,
        long sequence,
        double raceTimeSeconds,
        double raceDistanceMeters,
        double lapLengthMeters,
        int totalLaps,
        List<SimulationParticipantResponse> participants,
        List<TelemetryHorse> horses,
        List<UUID> ranking,
        List<String> publicEvents,
        List<ProvisionalResultResponse> provisionalResults) {
}
