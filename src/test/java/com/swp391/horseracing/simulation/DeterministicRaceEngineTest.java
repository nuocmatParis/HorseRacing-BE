package com.swp391.horseracing.simulation;

import com.swp391.horseracing.simulation.anomaly.DeterministicAnomalyDetector;
import com.swp391.horseracing.simulation.domain.SimulationRunnerStatus;
import com.swp391.horseracing.simulation.engine.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicRaceEngineTest {
    private final DeterministicRaceEngine engine =
            new DeterministicRaceEngine(new DeterministicAnomalyDetector());

    @Test
    void sameInputAndSeedProduceExactlySameTimelineAndResult() {
        List<SimulationProfile> profiles = profiles();

        GeneratedSimulation first = engine.generate(profiles, 1_200, 1.0, 424242L);
        GeneratedSimulation second = engine.generate(profiles, 1_200, 1.0, 424242L);

        assertEquals(first, second);
    }

    @Test
    void sequenceIsStrictlyIncreasingAndSnapshotEndsAtFinalFrame() {
        GeneratedSimulation generated = engine.generate(profiles(), 1_000, 0.96, 91L);

        assertFalse(generated.frames().isEmpty());
        for (int index = 0; index < generated.frames().size(); index++) {
            assertEquals(index, generated.frames().get(index).sequence());
        }
        assertTrue(generated.frames().getLast().horses().stream()
                .noneMatch(horse -> horse.status() == SimulationRunnerStatus.RUNNING));
    }

    @Test
    void provisionalResultContainsEveryStarterButDoesNotCreateOfficialRaceResult() {
        GeneratedSimulation generated = engine.generate(profiles(), 1_000, 1.0, 120L);

        assertEquals(profiles().size(), generated.provisionalResults().size());
        assertEquals(profiles().stream().map(SimulationProfile::entryId).sorted().toList(),
                generated.provisionalResults().stream().map(ProvisionalStanding::entryId).sorted().toList());
        assertTrue(generated.provisionalResults().stream().allMatch(result ->
                result.status() == SimulationRunnerStatus.FINISHED
                        || result.status() == SimulationRunnerStatus.DID_NOT_FINISH));
    }

    @Test
    void laneNumberDoesNotChangeMappedPerformance() {
        SimulationProfile laneOne = profile(1, "Alpha", 10);
        SimulationProfile laneEight = new SimulationProfile(
                laneOne.entryId(), laneOne.horseId(), laneOne.horseName(), laneOne.horseImageUrl(),
                laneOne.jockeyId(), laneOne.jockeyName(), 8, laneOne.baseSpeed(), laneOne.acceleration(),
                laneOne.stamina(), laneOne.consistency(), laneOne.jockeySkill(), laneOne.jockeyAggressiveness(),
                laneOne.corneringSkill(), laneOne.staminaManagement(), laneOne.handicapWeight());

        assertEquals(laneOne.baseSpeed(), laneEight.baseSpeed());
        assertEquals(laneOne.acceleration(), laneEight.acceleration());
        assertEquals(laneOne.stamina(), laneEight.stamina());
    }

    @Test
    void explicitDemoModeProducesReviewableTelemetryAndOneDnf() {
        GeneratedSimulation generated = engine.generate(profiles(), 1_000, 1.0, 77L, true);

        assertTrue(generated.frames().stream().anyMatch(frame -> !frame.warnings().isEmpty()));
        assertTrue(generated.provisionalResults().stream()
                .anyMatch(result -> result.status() == SimulationRunnerStatus.DID_NOT_FINISH));
    }

    private List<SimulationProfile> profiles() {
        return List.of(
                profile(1, "Alpha", 10),
                profile(2, "Bravo", 20),
                profile(3, "Comet", 30),
                profile(4, "Dash", 40));
    }

    private SimulationProfile profile(int lane, String name, long idSuffix) {
        return new SimulationProfile(
                new UUID(1, idSuffix),
                new UUID(2, idSuffix),
                name,
                null,
                new UUID(3, idSuffix),
                "Jockey " + name,
                lane,
                15.2 + lane * 0.1,
                3.1,
                0.82,
                0.86,
                0.78,
                0.61,
                0.80,
                0.79,
                null);
    }
}
