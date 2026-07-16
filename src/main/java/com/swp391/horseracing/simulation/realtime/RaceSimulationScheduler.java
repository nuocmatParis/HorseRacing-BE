package com.swp391.horseracing.simulation.realtime;

import com.swp391.horseracing.simulation.engine.GeneratedSimulation;
import com.swp391.horseracing.simulation.engine.SimulationFrame;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class RaceSimulationScheduler {
    private final RaceSimulationFrameService frameService;
    private final ConcurrentMap<UUID, RuntimeState> runtimes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "race-simulation-frame");
        thread.setDaemon(true);
        return thread;
    });

    public void start(UUID sessionId, GeneratedSimulation simulation) {
        start(sessionId, simulation, 0);
    }

    public void start(UUID sessionId, GeneratedSimulation simulation, int startIndex) {
        RuntimeState runtime = new RuntimeState(simulation, startIndex);
        if (runtimes.putIfAbsent(sessionId, runtime) != null) {
            throw new IllegalStateException("A frame scheduler is already active for session " + sessionId);
        }
        runtime.future = executor.scheduleAtFixedRate(
                () -> tick(sessionId, runtime),
                0,
                500,
                TimeUnit.MILLISECONDS);
    }

    public boolean isRunning(UUID sessionId) {
        return runtimes.containsKey(sessionId);
    }

    private void tick(UUID sessionId, RuntimeState runtime) {
        try {
            int index = runtime.index.getAndIncrement();
            if (index >= runtime.simulation.frames().size()) {
                stop(sessionId, runtime);
                return;
            }
            SimulationFrame frame = runtime.simulation.frames().get(index);
            boolean last = index == runtime.simulation.frames().size() - 1;
            frameService.publish(sessionId, runtime.simulation, frame, last);
            if (last) stop(sessionId, runtime);
        } catch (Exception exception) {
            log.error("Live race frame publishing failed for sessionId={}", sessionId, exception);
            try {
                frameService.publishAborted(sessionId, "The real-time frame scheduler stopped unexpectedly.");
            } finally {
                stop(sessionId, runtime);
            }
        }
    }

    private void stop(UUID sessionId, RuntimeState runtime) {
        runtimes.remove(sessionId, runtime);
        if (runtime.future != null) runtime.future.cancel(false);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private static final class RuntimeState {
        private final GeneratedSimulation simulation;
        private final AtomicInteger index = new AtomicInteger();
        private volatile ScheduledFuture<?> future;

        private RuntimeState(GeneratedSimulation simulation, int startIndex) {
            this.simulation = simulation;
            this.index.set(Math.max(0, startIndex));
        }
    }
}
