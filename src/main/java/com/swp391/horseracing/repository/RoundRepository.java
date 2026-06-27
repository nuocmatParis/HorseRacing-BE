package com.swp391.horseracing.repository;

import com.swp391.horseracing.dto.tournament.response.RoundResponse;
import com.swp391.horseracing.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoundRepository extends JpaRepository<Round, UUID> {
    List<Round> findByTournament_TournamentIdOrderBySequenceOrderAsc(UUID tournamentId);
    boolean existsByTournament_TournamentIdAndSequenceOrder(UUID tournamentId, int sequenceOrder);
    boolean existsByTournament_TournamentIdAndRoundName(UUID tournamentId, String roundName);
    List<RoundResponse> findByTournament_TournamentId(UUID id);
}
