package com.swp391.horseracing.simulation.engine;

import com.swp391.horseracing.simulation.anomaly.DeterministicAnomalyDetector;
import com.swp391.horseracing.simulation.domain.SimulationRunnerStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/** Pure seeded engine. It does not access Spring state or the database. */
@Component
public class DeterministicRaceEngine {
    public static final double FRAME_SECONDS = 0.5;
    private static final double MAX_RACE_SECONDS = 420.0;

    private final DeterministicAnomalyDetector anomalyDetector;

    public DeterministicRaceEngine(DeterministicAnomalyDetector anomalyDetector) {
        this.anomalyDetector = anomalyDetector;
    }

    public GeneratedSimulation generate(
            List<SimulationProfile> profiles,
            double raceDistanceMeters,
            double trackFactor,
            long seed) {
        return generate(profiles, raceDistanceMeters, trackFactor, seed, false);
    }

    public GeneratedSimulation generate(
            List<SimulationProfile> profiles,
            double raceDistanceMeters,
            double trackFactor,
            long seed,
            boolean demoMode) {
        if (profiles == null || profiles.size() < 2) {
            throw new IllegalArgumentException("A simulation needs at least two participants");
        }
        if (raceDistanceMeters <= 0) {
            throw new IllegalArgumentException("Race distance must be positive");
        }

        int totalLaps = Math.max(2, (int) Math.ceil(raceDistanceMeters / 500.0));
        double lapLength = raceDistanceMeters / totalLaps;
        List<Runner> runners = java.util.stream.IntStream.range(0, profiles.size())
                .mapToObj(index -> new Runner(
                        profiles.get(index),
                        seed,
                        demoMode && index == profiles.size() - 1,
                        demoMode && index == 0))
                .toList();
        List<SimulationFrame> frames = new ArrayList<>();
        long sequence = 0;
        double time = 0.0;
        frames.add(frame(sequence++, time, runners, lapLength, List.of("Horses are ready at the starting gate.")));

        while (time < MAX_RACE_SECONDS && runners.stream().anyMatch(Runner::active)) {
            time = round(time + FRAME_SECONDS);
            final double frameTime = time;
            List<String> events = new ArrayList<>();
            Map<Runner, DetectedWarning> warnings = new LinkedHashMap<>();

            for (Runner runner : runners) {
                if (!runner.active()) continue;
                double previousSpeed = runner.speed;
                double previousEnergy = runner.energy;
                double lapProgress = (runner.distance % lapLength) / lapLength;
                boolean inCurve = (lapProgress >= 0.18 && lapProgress <= 0.36)
                        || (lapProgress >= 0.68 && lapProgress <= 0.86);
                runner.advance(FRAME_SECONDS, raceDistanceMeters, trackFactor, inCurve, time);

                anomalyDetector.detect(
                                runner.profile,
                                previousSpeed,
                                runner.speed,
                                previousEnergy,
                                runner.energy,
                                FRAME_SECONDS,
                                inCurve)
                                .filter(warning -> runner.canEmit(warning.type(), frameTime))
                        .ifPresent(warning -> {
                            warnings.put(runner, warning);
                            runner.markEmitted(warning.type(), frameTime);
                        });

                if (runner.justFinished) {
                    events.add(runner.profile.horseName() + " crossed the finish line.");
                    runner.justFinished = false;
                } else if (runner.justDnf) {
                    events.add(runner.profile.horseName() + " did not finish.");
                    runner.justDnf = false;
                }
            }
            frames.add(frame(sequence++, time, runners, lapLength, events, warnings.values().stream().toList()));
        }

        List<Runner> finished = runners.stream()
                .filter(runner -> runner.status == SimulationRunnerStatus.FINISHED)
                .sorted(Comparator.comparingDouble(runner -> runner.finishTime))
                .toList();
        Map<UUID, Integer> finalRanks = new HashMap<>();
        for (int i = 0; i < finished.size(); i++) {
            finalRanks.put(finished.get(i).profile.entryId(), i + 1);
        }
        List<ProvisionalStanding> standings = runners.stream()
                .sorted(Comparator
                        .comparing((Runner runner) -> runner.status != SimulationRunnerStatus.FINISHED)
                        .thenComparingDouble(runner -> runner.status == SimulationRunnerStatus.FINISHED
                                ? runner.finishTime : -runner.distance))
                .map(runner -> new ProvisionalStanding(
                        runner.profile.entryId(),
                        finalRanks.get(runner.profile.entryId()),
                        runner.status == SimulationRunnerStatus.FINISHED ? round(runner.finishTime) : null,
                        runner.status))
                .toList();
        return new GeneratedSimulation(raceDistanceMeters, lapLength, totalLaps, List.copyOf(frames), standings);
    }

    private SimulationFrame frame(long sequence, double time, List<Runner> runners, double lapLength, List<String> events) {
        return frame(sequence, time, runners, lapLength, events, List.of());
    }

    private SimulationFrame frame(
            long sequence,
            double time,
            List<Runner> runners,
            double lapLength,
            List<String> events,
            List<DetectedWarning> warnings) {
        List<Runner> ranking = runners.stream().sorted(Comparator
                .comparingInt((Runner runner) -> switch (runner.status) {
                    case FINISHED -> 0;
                    case RUNNING, READY -> 1;
                    case DID_NOT_FINISH -> 2;
                })
                .thenComparingDouble(runner -> runner.status == SimulationRunnerStatus.FINISHED
                        ? runner.finishTime : -runner.distance)).toList();
        Map<UUID, Integer> rankByEntry = new HashMap<>();
        for (int i = 0; i < ranking.size(); i++) rankByEntry.put(ranking.get(i).profile.entryId(), i + 1);

        List<TelemetryHorse> horses = runners.stream().map(runner -> new TelemetryHorse(
                runner.profile.entryId(),
                runner.profile.horseId(),
                runner.profile.horseName(),
                runner.profile.horseImageUrl(),
                runner.profile.jockeyName(),
                runner.profile.laneNumber(),
                Math.max(1, (int) Math.ceil(runner.distance / lapLength)),
                round(runner.distance),
                round(runner.speed),
                round(runner.energy),
                rankByEntry.get(runner.profile.entryId()),
                runner.status)).toList();
        return new SimulationFrame(sequence, time, horses, warnings, List.copyOf(events));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static final class Runner {
        private final SimulationProfile profile;
        private final Random random;
        private final double dnfAt;
        private final boolean demoAnomaly;
        private final Map<Object, Double> warningTimes = new HashMap<>();
        private double distance;
        private double speed;
        private double energy = 100.0;
        private double finishTime;
        private SimulationRunnerStatus status = SimulationRunnerStatus.RUNNING;
        private boolean justFinished;
        private boolean justDnf;

        private Runner(SimulationProfile profile, long seed, boolean forceDemoDnf, boolean demoAnomaly) {
            this.profile = profile;
            long runnerSeed = seed ^ profile.entryId().getMostSignificantBits() ^ profile.entryId().getLeastSignificantBits();
            this.random = new Random(runnerSeed);
            long bucket = Math.floorMod(runnerSeed, 31);
            this.dnfAt = forceDemoDnf
                    ? 0.68
                    : bucket == 0 ? 0.58 + random.nextDouble() * 0.25 : Double.POSITIVE_INFINITY;
            this.demoAnomaly = demoAnomaly;
        }

        private boolean active() {
            return status == SimulationRunnerStatus.RUNNING;
        }

        private void advance(double dt, double totalDistance, double trackFactor, boolean inCurve, double time) {
            double progress = distance / totalDistance;
            double fatigue = energy < 35.0 ? (35.0 - energy) / 100.0 : 0.0;
            double curvePenalty = inCurve ? (1.0 - profile.corneringSkill()) * 0.10 : 0.0;
            double sprint = progress > 0.80 ? profile.jockeyAggressiveness() * 0.06 : 0.0;
            double variance = random.nextGaussian() * (1.0 - profile.consistency()) * 0.65;
            double burst = random.nextDouble() < 0.006 ? profile.baseSpeed() * (0.12 + random.nextDouble() * 0.10) : 0.0;
            double target = profile.baseSpeed()
                    * trackFactor
                    * (0.94 + profile.jockeySkill() * 0.08 + sprint - fatigue - curvePenalty)
                    + variance + burst;
            double maxIncrease = profile.acceleration() * dt;
            double maxDecrease = Math.max(0.45, profile.acceleration() * 0.60) * dt;
            speed += Math.max(-maxDecrease, Math.min(maxIncrease, target - speed));
            if (burst > 0.0) {
                // A seeded performance burst is part of telemetry generation. The detector
                // independently decides whether the resulting measurements are abnormal.
                speed += burst * 0.65;
            }
            if (demoAnomaly && time >= 12.0 && time < 12.0 + dt) {
                // Demo mode changes telemetry, not detector output. The normal deterministic
                // detector still decides whether this physical spike is review-worthy.
                speed += profile.baseSpeed() * 0.18;
            }
            speed = Math.max(0.0, speed);
            distance = Math.min(totalDistance, distance + speed * dt);

            double effort = speed / Math.max(1.0, profile.baseSpeed());
            double energyCost = (0.32 + effort * 0.20 + profile.jockeyAggressiveness() * 0.05)
                    * (1.12 - profile.stamina() * 0.35)
                    * (1.10 - profile.staminaManagement() * 0.20) * dt;
            energy = Math.max(0.0, energy - energyCost);

            if (progress >= dnfAt) {
                status = SimulationRunnerStatus.DID_NOT_FINISH;
                speed = 0.0;
                justDnf = true;
            } else if (distance >= totalDistance) {
                status = SimulationRunnerStatus.FINISHED;
                finishTime = time;
                speed = 0.0;
                justFinished = true;
            }
        }

        private boolean canEmit(Object type, double time) {
            return time - warningTimes.getOrDefault(type, -100.0) >= 8.0;
        }

        private void markEmitted(Object type, double time) {
            warningTimes.put(type, time);
        }
    }
}
