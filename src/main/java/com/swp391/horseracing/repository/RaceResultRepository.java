package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.RaceResult;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

@Repository
public interface RaceResultRepository extends JpaRepository<RaceResult, UUID> {

    List<RaceResult> findByRace_RaceIdOrderByRankAsc(UUID raceId);

    Optional<RaceResult> findByEntry_EntryId(UUID entryId);

    boolean existsByRace_RaceIdAndEntry_EntryId(UUID raceId, UUID entryId);

    boolean existsByRace_RaceIdAndRank(UUID raceId, int rank);

    boolean existsByRace_RaceId(UUID raceId);

    int countByRace_RaceId(UUID raceId);

    List<RaceResult> findByRace_RaceId(UUID raceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RaceResult> findForUpdateByRace_RaceId(UUID raceId);

    @Query("SELECT DISTINCT result FROM RaceResult result "
            + "JOIN FETCH result.race race "
            + "JOIN FETCH race.round round "
            + "JOIN FETCH round.tournament tournament "
            + "JOIN FETCH result.entry entry "
            + "JOIN FETCH entry.contract contract "
            + "JOIN FETCH contract.horse horse "
            + "JOIN FETCH contract.jockey jockey "
            + "JOIN FETCH jockey.user jockeyUser "
            + "JOIN FETCH contract.owner owner "
            + "JOIN FETCH owner.user ownerUser "
            + "WHERE result.resultId IN :resultIds")
    List<RaceResult> findAllWithTransactionContextByResultIdIn(
            @Param("resultIds") Collection<UUID> resultIds);
}
