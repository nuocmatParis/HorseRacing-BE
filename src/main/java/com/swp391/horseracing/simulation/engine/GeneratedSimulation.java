package com.swp391.horseracing.simulation.engine;

import java.util.List;

public record GeneratedSimulation(
        double raceDistanceMeters,
        double lapLengthMeters,
        int totalLaps,
        List<SimulationFrame> frames,
        List<ProvisionalStanding> provisionalResults) {
}
