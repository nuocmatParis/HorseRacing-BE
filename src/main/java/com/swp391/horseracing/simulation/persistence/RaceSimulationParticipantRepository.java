package com.swp391.horseracing.simulation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RaceSimulationParticipantRepository extends JpaRepository<RaceSimulationParticipant, UUID> {
    List<RaceSimulationParticipant> findBySession_SessionIdOrderByLaneNumberAsc(UUID sessionId);
}
