package com.swp391.horseracing.simulation;

import tools.jackson.databind.ObjectMapper;
import com.swp391.horseracing.simulation.api.LiveRaceSnapshotResponse;
import com.swp391.horseracing.simulation.domain.SimulationStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PublicLiveRaceDtoPrivacyTest {
    @Test
    void publicSnapshotSchemaCannotExposeWarningsRiskOrRefereeNotes() throws Exception {
        LiveRaceSnapshotResponse response = new LiveRaceSnapshotResponse(
                UUID.randomUUID(), "Race", "Tournament", UUID.randomUUID(),
                SimulationStatus.RUNNING, 12L, 6.0, 1_000, 500, 2,
                List.of(), List.of(), List.of(), List.of(), List.of());

        String json = new ObjectMapper().writeValueAsString(response);

        assertFalse(json.contains("warning"));
        assertFalse(json.contains("riskScore"));
        assertFalse(json.contains("reviewNote"));
        assertFalse(json.contains("flag"));
    }
}
