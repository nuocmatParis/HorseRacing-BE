package com.swp391.horseracing.simulation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RaceProvisionalResultRepository extends JpaRepository<RaceProvisionalResult, UUID> {
    List<RaceProvisionalResult> findByRace_RaceIdOrderByFinishPositionAsc(UUID raceId);

    boolean existsBySession_SessionId(UUID sessionId);
}
