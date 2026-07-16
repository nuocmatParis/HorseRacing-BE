package com.swp391.horseracing.simulation;

import com.swp391.horseracing.entity.*;
import com.swp391.horseracing.enums.*;
import com.swp391.horseracing.simulation.engine.SimulationProfile;
import com.swp391.horseracing.simulation.mapper.SimulationProfileMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SimulationProfileMapperTest {
    private final SimulationProfileMapper mapper = new SimulationProfileMapper();

    @Test
    void mapsProductionEntryIdsNamesAndLaneIntoBoundedProfile() {
        RaceEntry entry = entry(6);

        SimulationProfile profile = mapper.map(entry, null);

        assertEquals(entry.getEntryId(), profile.entryId());
        assertEquals(entry.getContract().getHorse().getHorseId(), profile.horseId());
        assertEquals(entry.getContract().getJockey().getJockeyId(), profile.jockeyId());
        assertEquals(6, profile.laneNumber());
        assertTrue(profile.baseSpeed() >= 12.5 && profile.baseSpeed() <= 18.2);
        assertTrue(profile.consistency() >= 0.55 && profile.consistency() <= 0.97);
        assertTrue(profile.jockeySkill() >= 0.45 && profile.jockeySkill() <= 0.98);
    }

    @Test
    void changingOnlyLaneDoesNotCreatePerformanceAdvantage() {
        SimulationProfile first = mapper.map(entry(1), null);
        SimulationProfile eighth = mapper.map(entry(8), null);

        assertEquals(first.baseSpeed(), eighth.baseSpeed());
        assertEquals(first.acceleration(), eighth.acceleration());
        assertEquals(first.stamina(), eighth.stamina());
        assertEquals(first.jockeySkill(), eighth.jockeySkill());
    }

    private RaceEntry entry(int lane) {
        Horse horse = Horse.builder()
                .horseId(new UUID(1, 1))
                .name("Thunder")
                .age(5)
                .weight(470)
                .currentRating(92)
                .raceClass(RaceClass.CLASS_2)
                .totalRaces(28)
                .totalWins(7)
                .winRate(25.0)
                .build();
        User jockeyUser = User.builder().userId(new UUID(2, 1)).fullName("Nguyen Van A").build();
        Jockey jockey = Jockey.builder()
                .jockeyId(new UUID(3, 1))
                .user(jockeyUser)
                .experienceYears(7)
                .totalRaces(120)
                .totalWins(24)
                .jockeyTier(JockeyTier.PROFESSIONAL)
                .specialization(Specialization.SPRINT)
                .build();
        Race race = Race.builder().raceId(new UUID(4, 1)).distance(RaceDistance.SPRINT_1000M).build();
        JockeyHorseContract contract = JockeyHorseContract.builder().horse(horse).jockey(jockey).build();
        return RaceEntry.builder()
                .entryId(UUID.randomUUID())
                .race(race)
                .contract(contract)
                .laneNumber(lane)
                .status(RaceEntryStatus.CONFIRMED)
                .build();
    }
}
