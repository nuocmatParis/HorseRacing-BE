package com.swp391.horseracing.simulation.anomaly;

import com.swp391.horseracing.simulation.domain.SimulationSeverity;
import com.swp391.horseracing.simulation.domain.SimulationWarningType;
import com.swp391.horseracing.simulation.engine.DetectedWarning;
import com.swp391.horseracing.simulation.engine.SimulationProfile;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Rule-based detector. It has no random branch: identical telemetry always produces
 * the same warning decision and bounded risk score.
 */
@Component
public class DeterministicAnomalyDetector {
    public Optional<DetectedWarning> detect(
            SimulationProfile profile,
            double previousSpeed,
            double speed,
            double previousEnergy,
            double energy,
            double deltaSeconds,
            boolean inCurve) {
        double speedIncrease = Math.max(0.0, speed - previousSpeed);
        double acceleration = speedIncrease / Math.max(0.1, deltaSeconds);
        double energyDrop = Math.max(0.0, previousEnergy - energy);
        double expectedIncrease = profile.acceleration() * Math.max(0.1, deltaSeconds);

        List<Candidate> candidates = List.of(
                new Candidate(SimulationWarningType.ABNORMAL_SPEED_SPIKE,
                        clamp((speedIncrease - expectedIncrease * 1.10)
                                / Math.max(0.5, profile.baseSpeed() * 0.12))),
                new Candidate(SimulationWarningType.UNREALISTIC_ACCELERATION,
                        clamp((acceleration / Math.max(0.5, profile.acceleration()) - 1.05) / 0.75)),
                new Candidate(SimulationWarningType.STAMINA_DROP_TOO_FAST,
                        clamp(energyDrop / 0.85)),
                new Candidate(SimulationWarningType.CURVE_SPEED_ABNORMAL,
                        inCurve ? clamp((speed / Math.max(1.0, profile.baseSpeed()) - 0.98) / 0.16) : 0.0),
                new Candidate(SimulationWarningType.PERFORMANCE_OUTLIER,
                        clamp((speed / Math.max(1.0, profile.baseSpeed()) - 1.08) / 0.18))
        );

        Candidate strongest = candidates.stream().max(Comparator.comparingDouble(Candidate::risk)).orElseThrow();
        if (strongest.risk() < 0.45) {
            return Optional.empty();
        }
        double risk = round(strongest.risk());
        return Optional.of(new DetectedWarning(
                profile.entryId(),
                profile.horseId(),
                strongest.type(),
                severity(risk),
                risk,
                message(strongest.type()),
                "Review telemetry and decide whether to ignore or flag the incident."));
    }

    private SimulationSeverity severity(double risk) {
        if (risk >= 0.90) return SimulationSeverity.CRITICAL;
        if (risk >= 0.70) return SimulationSeverity.HIGH;
        return SimulationSeverity.MEDIUM;
    }

    private String message(SimulationWarningType type) {
        return switch (type) {
            case ABNORMAL_SPEED_SPIKE -> "Speed increased unusually within one telemetry interval.";
            case UNREALISTIC_ACCELERATION -> "Measured acceleration exceeds this horse's mapped profile.";
            case STAMINA_DROP_TOO_FAST -> "Energy decreased faster than the mapped stamina profile allows.";
            case CURVE_SPEED_ABNORMAL -> "The horse maintained an abnormal speed through a curve.";
            case PERFORMANCE_OUTLIER -> "Current performance is an outlier against the mapped baseline.";
        };
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record Candidate(SimulationWarningType type, double risk) {
    }
}
