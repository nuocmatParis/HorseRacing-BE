package com.swp391.horseracing.simulation.engine;

import java.util.List;

public record SimulationFrame(
        long sequence,
        double raceTimeSeconds,
        List<TelemetryHorse> horses,
        List<DetectedWarning> warnings,
        List<String> publicEvents) {
}
