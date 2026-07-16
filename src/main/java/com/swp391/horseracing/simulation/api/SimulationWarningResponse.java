package com.swp391.horseracing.simulation.api;

import com.swp391.horseracing.simulation.domain.*;

import java.time.LocalDateTime;
import java.util.UUID;

public record SimulationWarningResponse(
        UUID warningId,
        UUID entryId,
        UUID horseId,
        String horseName,
        SimulationWarningType warningType,
        SimulationSeverity severity,
        double riskScore,
        double raceTimeSeconds,
        String message,
        String suggestedAction,
        WarningReviewStatus reviewStatus,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String reviewNote) {
}
