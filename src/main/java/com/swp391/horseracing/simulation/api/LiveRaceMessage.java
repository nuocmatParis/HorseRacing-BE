package com.swp391.horseracing.simulation.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record LiveRaceMessage(
        String type,
        UUID raceId,
        UUID sessionId,
        long sequence,
        double raceTimeSeconds,
        LocalDateTime serverTime,
        Object payload) {
}
