package com.swp391.horseracing.simulation.realtime;

import com.swp391.horseracing.dto.tournament.response.RaceResponse;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.service.RaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaceSimulationOrchestrator {
    private final RaceService raceService;
    private final RaceSimulationLifecycleService lifecycleService;
    private final RaceSimulationScheduler scheduler;

    public RaceResponse startRace(UUID raceId) {
        lifecycleService.requireReady(raceId);
        RaceResponse race = raceService.startRace(raceId);
        RaceSimulationLifecycleService.RuntimePlan plan;
        try {
            plan = lifecycleService.begin(raceId);
            scheduler.start(plan.sessionId(), plan.generated());
        } catch (Exception exception) {
            lifecycleService.abort(raceId);
            log.error("Simulation startup failed for raceId={}", raceId, exception);
            throw new AppException(ErrorCode.SIMULATION_START_FAILED);
        }
        return race;
    }
}
