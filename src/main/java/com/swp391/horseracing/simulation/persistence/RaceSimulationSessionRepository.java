package com.swp391.horseracing.simulation.persistence;

import com.swp391.horseracing.simulation.domain.SimulationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

public interface RaceSimulationSessionRepository extends JpaRepository<RaceSimulationSession, UUID> {
    Optional<RaceSimulationSession> findByRace_RaceId(UUID raceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RaceSimulationSession s where s.race.raceId = :raceId")
    Optional<RaceSimulationSession> findForUpdateByRaceId(@Param("raceId") UUID raceId);

    List<RaceSimulationSession> findByStatusOrderByStartedAtAsc(SimulationStatus status);

    Optional<RaceSimulationSession> findFirstByStatusAndFinishedAtAfterOrderByFinishedAtDesc(
            SimulationStatus status, LocalDateTime after);
}
