package com.swp391.horseracing.simulation.mapper;

import com.swp391.horseracing.entity.Horse;
import com.swp391.horseracing.entity.HorseInspection;
import com.swp391.horseracing.entity.Jockey;
import com.swp391.horseracing.entity.RaceEntry;
import com.swp391.horseracing.enums.JockeyTier;
import com.swp391.horseracing.enums.Specialization;
import com.swp391.horseracing.simulation.engine.SimulationProfile;
import org.springframework.stereotype.Component;

/**
 * Deterministically maps the production horse/jockey model into bounded simulator
 * attributes. Lane is copied as identity only and never contributes to performance.
 */
@Component
public class SimulationProfileMapper {
    public SimulationProfile map(RaceEntry entry, HorseInspection inspection) {
        Horse horse = entry.getContract().getHorse();
        Jockey jockey = entry.getContract().getJockey();

        double rating = clamp(horse.getCurrentRating() / 150.0);
        double horseWinRate = normalizeRate(horse.getWinRate());
        double horseExperience = clamp(horse.getTotalRaces() / 80.0);
        double agePrime = clamp(1.0 - Math.abs(horse.getAge() - 5.0) / 7.0);
        double weightFit = clamp(1.0 - Math.abs(horse.getWeight() - 470.0) / 260.0);

        double jockeyWinRate = jockey.getTotalRaces() <= 0
                ? 0.35
                : clamp((double) jockey.getTotalWins() / jockey.getTotalRaces());
        double tier = tierScore(jockey.getJockeyTier());
        double jockeyExperience = clamp(jockey.getExperienceYears() / 15.0);
        double specializationMatch = specializationMatches(jockey.getSpecialization(),
                entry.getRace().getDistance().getCategory()) ? 1.0 : 0.55;

        // Speeds are metres/second. Every output remains within a documented physical
        // range suitable for the academic simulator rather than being keyed by IDs.
        double baseSpeed = bounded(13.0 + 4.5 * (rating * 0.45 + horseWinRate * 0.25
                + agePrime * 0.15 + weightFit * 0.15), 12.5, 18.2);
        double acceleration = bounded(2.2 + 2.0 * (rating * 0.35 + agePrime * 0.35
                + horseWinRate * 0.30), 2.0, 4.5);
        double stamina = bounded(0.55 + 0.40 * (horseExperience * 0.30 + rating * 0.30
                + specializationMatch * 0.25 + weightFit * 0.15), 0.50, 0.96);
        double consistency = bounded(0.58 + 0.38 * (horseExperience * 0.50
                + horseWinRate * 0.30 + rating * 0.20), 0.55, 0.97);
        double jockeySkill = bounded(0.48 + 0.48 * (tier * 0.45 + jockeyWinRate * 0.30
                + jockeyExperience * 0.25), 0.45, 0.98);
        double aggressiveness = bounded(0.38 + 0.40 * tier + 0.18 * (1.0 - jockeyExperience),
                0.35, 0.92);
        double cornering = bounded(0.48 + 0.46 * (jockeySkill * 0.55
                + specializationMatch * 0.45), 0.45, 0.97);
        double staminaManagement = bounded(0.50 + 0.44 * (jockeySkill * 0.55
                + jockeyExperience * 0.45), 0.48, 0.97);

        return new SimulationProfile(
                entry.getEntryId(),
                horse.getHorseId(),
                horse.getName(),
                horse.getImageUrl(),
                jockey.getJockeyId(),
                jockey.getUser().getFullName(),
                entry.getLaneNumber(),
                round(baseSpeed),
                round(acceleration),
                round(stamina),
                round(consistency),
                round(jockeySkill),
                round(aggressiveness),
                round(cornering),
                round(staminaManagement),
                inspection == null || inspection.getHandicapWeight() == null
                        ? null : inspection.getHandicapWeight().doubleValue());
    }

    private boolean specializationMatches(Specialization actual, Specialization expected) {
        return actual != null && actual == expected;
    }

    private double tierScore(JockeyTier tier) {
        if (tier == null) return 0.25;
        return switch (tier) {
            case APPRENTICE -> 0.25;
            case JUNIOR -> 0.50;
            case PROFESSIONAL -> 0.78;
            case ELITE -> 1.0;
        };
    }

    private double normalizeRate(Double rate) {
        if (rate == null) return 0.0;
        return clamp(rate > 1.0 ? rate / 100.0 : rate);
    }

    private double bounded(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value) {
        return bounded(value, 0.0, 1.0);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
