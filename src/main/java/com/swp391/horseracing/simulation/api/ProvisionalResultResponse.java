package com.swp391.horseracing.simulation.api;

import com.swp391.horseracing.simulation.domain.SimulationRunnerStatus;

import java.util.UUID;

public record ProvisionalResultResponse(
        UUID entryId,
        UUID horseId,
        String horseName,
        String jockeyName,
        int laneNumber,
        Integer finishPosition,
        Double finishTime,
        SimulationRunnerStatus status) {
}
