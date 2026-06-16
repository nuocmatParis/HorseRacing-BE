package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoundRepository extends JpaRepository<Round, UUID> {
    List<Round> findByTournament_TournamentIdOrderBySequenceOrderAsc(UUID tournamentId);
}
