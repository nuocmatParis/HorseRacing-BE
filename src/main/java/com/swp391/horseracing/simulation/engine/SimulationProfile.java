package com.swp391.horseracing.simulation.engine;

import java.util.UUID;

public record SimulationProfile(
        UUID entryId,
        UUID horseId,
        String horseName,
        String horseImageUrl,
        UUID jockeyId,
        String jockeyName,
        int laneNumber,
        double baseSpeed,
        double acceleration,
        double stamina,
        double consistency,
        double jockeySkill,
        double jockeyAggressiveness,
        double corneringSkill,
        double staminaManagement,
        Double handicapWeight) {
}
