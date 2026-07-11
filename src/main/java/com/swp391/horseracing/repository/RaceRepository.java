package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swp391.horseracing.enums.RoundStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RaceRepository extends JpaRepository<Race, UUID> {

    List<Race> findByRound_RoundId(UUID roundId);
    List<Race> findByRound_RoundIdOrderByStartTimeDesc(UUID roundId);
    boolean existsByRound_RoundIdAndName(UUID roundId, String name);
    boolean existsByRound_RoundIdAndSequenceOrder(UUID roundId, int sequenceOrder);
    List<Race> findByRound_RoundIdAndRaceIdNotOrderBySequenceOrderAsc(UUID roundId, UUID raceId);
    List<Race> findByRound_Tournament_TournamentId(UUID tournamentId);

    long countByRound_Tournament_TournamentIdAndStartTimeBetweenAndStatusNot(
            UUID tournamentId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            RoundStatus status
    );

    long countByRound_Tournament_TournamentIdAndStartTimeBetweenAndStatusNotAndRaceIdNot(
            UUID tournamentId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            RoundStatus status,
            UUID raceId
    );
}