package com.swp391.horseracing.simulation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RaceSimulationFlagRepository extends JpaRepository<RaceSimulationFlag, UUID> {
    List<RaceSimulationFlag> findByRace_RaceIdOrderByFlaggedAtDesc(UUID raceId);

    boolean existsByWarning_WarningId(UUID warningId);
}
