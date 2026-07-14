package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.RaceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RaceEntryRepository extends JpaRepository<RaceEntry, UUID> {

    List<RaceEntry> findByRace_RaceIdOrderByLaneNumberAsc(UUID raceId);

    List<RaceEntry> findByRace_RaceIdOrderByCreatedAtAsc(UUID raceId);

    boolean existsByRace_RaceIdAndLaneNumber(UUID raceId, int laneNumber);

    boolean existsByRace_RaceIdAndContract_ContractId(UUID raceId, UUID contractId);

    List<RaceEntry> findByRace_Round_RoundId(UUID roundId);

    int countByRace_Round_RoundId(UUID roundId);

    int countByRace_RaceId(UUID raceId);

    List<RaceEntry> findByContract_Horse_HorseId(UUID horseId);

    List<RaceEntry> findByContract_Jockey_JockeyId(UUID jockeyId);

    List<RaceEntry> findByContract_ContractId(UUID contractId);

    List<RaceEntry> findByRace_RaceIdAndContract_Owner_User_UserIdOrderByLaneNumberAsc(UUID raceId, UUID userId);

    List<RaceEntry> findByRace_RaceIdAndContract_Jockey_User_UserIdOrderByLaneNumberAsc(UUID raceId, UUID userId);
}
