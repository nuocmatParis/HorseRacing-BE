package com.swp391.horseracing.simulation.engine;

import com.swp391.horseracing.simulation.domain.SimulationSeverity;
import com.swp391.horseracing.simulation.domain.SimulationWarningType;

import java.util.UUID;

public record DetectedWarning(
        UUID entryId,
        UUID horseId,
        SimulationWarningType type,
        SimulationSeverity severity,
        double riskScore,
        String message,
        String suggestedAction) {
}
