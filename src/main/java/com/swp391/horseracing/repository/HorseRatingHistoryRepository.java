package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.HorseRatingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HorseRatingHistoryRepository extends JpaRepository<HorseRatingHistory, UUID> {
    Optional<HorseRatingHistory> findByRaceResult_ResultId(UUID raceResultId);
    List<HorseRatingHistory> findByRace_RaceId(UUID raceId);
    List<HorseRatingHistory> findByHorse_HorseIdOrderByCalculatedAtAsc(UUID horseId);
    boolean existsByRaceResult_ResultId(UUID raceResultId);
    List<HorseRatingHistory> findByRace_Round_RoundId(UUID roundId);
}
