package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.TournamentPhaseConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TournamentPhaseConfigRepository extends JpaRepository<TournamentPhaseConfig, Long> {

    List<TournamentPhaseConfig> findByTournamentTournamentId(UUID tournamentId);

    java.util.Optional<TournamentPhaseConfig> findByTournamentTournamentIdAndPhaseName(
            UUID tournamentId, String phaseName);

    void deleteByTournamentTournamentId(UUID tournamentId);
}
