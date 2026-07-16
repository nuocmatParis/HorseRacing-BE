package com.swp391.horseracing.simulation.api;

import java.util.UUID;

public record LiveRaceSummaryResponse(
        UUID raceId,
        UUID sessionId,
        String raceName,
        String tournamentName,
        double raceTimeSeconds,
        double progressPercent,
        String leaderName) {
}
