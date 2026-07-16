package com.swp391.horseracing.simulation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RaceSimulationWarningRepository extends JpaRepository<RaceSimulationWarning, UUID> {
    List<RaceSimulationWarning> findByRace_RaceIdOrderByCreatedAtDesc(UUID raceId);

    boolean existsBySession_SessionIdAndEntry_EntryIdAndWarningTypeAndSequence(
            UUID sessionId,
            UUID entryId,
            com.swp391.horseracing.simulation.domain.SimulationWarningType warningType,
            Long sequence);
}
