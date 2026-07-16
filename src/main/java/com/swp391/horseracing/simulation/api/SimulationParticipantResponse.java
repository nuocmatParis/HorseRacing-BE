package com.swp391.horseracing.simulation.api;

import java.util.UUID;

public record SimulationParticipantResponse(
        UUID entryId,
        UUID horseId,
        String horseName,
        String horseImageUrl,
        UUID jockeyId,
        String jockeyName,
        int laneNumber) {
}
