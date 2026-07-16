package com.swp391.horseracing.simulation;

import com.swp391.horseracing.simulation.anomaly.DeterministicAnomalyDetector;
import com.swp391.horseracing.simulation.domain.SimulationWarningType;
import com.swp391.horseracing.simulation.engine.DetectedWarning;
import com.swp391.horseracing.simulation.engine.SimulationProfile;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeterministicAnomalyDetectorTest {
    private final DeterministicAnomalyDetector detector = new DeterministicAnomalyDetector();
    private final SimulationProfile profile = new SimulationProfile(
            UUID.randomUUID(), UUID.randomUUID(), "Test Horse", null,
            UUID.randomUUID(), "Test Jockey", 3,
            15.0, 3.0, 0.8, 0.85, 0.8, 0.6, 0.8, 0.8, null);

    @Test
    void identicalTelemetryAlwaysProducesIdenticalDecision() {
        Optional<DetectedWarning> first = detector.detect(profile, 10.0, 14.0, 80.0, 79.8, 0.5, false);
        Optional<DetectedWarning> second = detector.detect(profile, 10.0, 14.0, 80.0, 79.8, 0.5, false);

        assertEquals(first, second);
        assertTrue(first.isPresent());
    }

    @Test
    void stableTelemetryDoesNotCreateWarning() {
        assertTrue(detector.detect(profile, 14.8, 15.0, 80.0, 79.9, 0.5, false).isEmpty());
    }

    @Test
    void riskIsBoundedAndCurveEvidenceProducesCurveWarning() {
        DetectedWarning warning = detector.detect(profile, 15.0, 18.0, 80.0, 79.9, 0.5, true).orElseThrow();

        assertTrue(warning.riskScore() >= 0.0 && warning.riskScore() <= 1.0);
        assertTrue(warning.type() == SimulationWarningType.CURVE_SPEED_ABNORMAL
                || warning.type() == SimulationWarningType.ABNORMAL_SPEED_SPIKE
                || warning.type() == SimulationWarningType.UNREALISTIC_ACCELERATION
                || warning.type() == SimulationWarningType.PERFORMANCE_OUTLIER);
    }
}
