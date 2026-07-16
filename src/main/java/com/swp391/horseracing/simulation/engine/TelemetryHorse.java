package com.swp391.horseracing.simulation.engine;

import com.swp391.horseracing.simulation.domain.SimulationRunnerStatus;

import java.util.UUID;

public record TelemetryHorse(
        UUID entryId,
        UUID horseId,
        String horseName,
        String horseImageUrl,
        String jockeyName,
        int laneNumber,
        int lapNumber,
        double distance,
        double speed,
        double energy,
        int rank,
        SimulationRunnerStatus status) {
}
