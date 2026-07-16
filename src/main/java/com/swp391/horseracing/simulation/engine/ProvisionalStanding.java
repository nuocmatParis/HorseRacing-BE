package com.swp391.horseracing.simulation.engine;

import com.swp391.horseracing.simulation.domain.SimulationRunnerStatus;

import java.util.UUID;

public record ProvisionalStanding(
        UUID entryId,
        Integer finishPosition,
        Double finishTime,
        SimulationRunnerStatus status) {
}
