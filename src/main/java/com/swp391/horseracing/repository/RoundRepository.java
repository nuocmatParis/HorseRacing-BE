package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoundRepository extends JpaRepository<Round, UUID> {
    List<Round> findByTournament_TournamentIdOrderBySequenceOrderAsc(UUID tournamentId);
    boolean existsByTournament_TournamentIdAndSequenceOrder(UUID tournamentId, int sequenceOrder);
    boolean existsByTournament_TournamentIdAndRoundName(UUID tournamentId, String roundName);
    boolean existsByTournament_TournamentIdAndIsFinalTrue(UUID tournamentId);
    java.util.Optional<Round> findByTournament_TournamentIdAndSequenceOrder(UUID tournamentId, int sequenceOrder);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Round r WHERE r.roundId = :roundId")
    java.util.Optional<Round> findForUpdateByRoundId(@Param("roundId") UUID roundId);
}
