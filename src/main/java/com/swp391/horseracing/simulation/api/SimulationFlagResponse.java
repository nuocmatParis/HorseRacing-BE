package com.swp391.horseracing.simulation.api;

import com.swp391.horseracing.simulation.domain.*;

import java.time.LocalDateTime;
import java.util.UUID;

public record SimulationFlagResponse(
        UUID flagId,
        UUID warningId,
        UUID entryId,
        UUID horseId,
        String horseName,
        FlagSource source,
        FlagReviewStatus status,
        SimulationSeverity severity,
        double raceTimeSeconds,
        String note,
        String flaggedByName,
        LocalDateTime flaggedAt,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String reviewNote,
        ViolationDraftResponse violationDraft) {
}
