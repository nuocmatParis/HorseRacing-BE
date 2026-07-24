package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Appeal;
import com.swp391.horseracing.enums.AppealStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppealRepository extends JpaRepository<Appeal, UUID> {

    List<Appeal> findByEntry_EntryIdAndStatus(UUID entryId, AppealStatus status);

    List<Appeal> findBySubmittedBy_UserId(UUID userId);

    List<Appeal> findByEntry_Race_RaceId(UUID raceId);

    boolean existsByEntry_Race_RaceIdAndStatus(UUID raceId, AppealStatus status);

    List<Appeal> findByEntry_Race_RaceIdOrderBySubmittedAtDesc(UUID raceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT appeal FROM Appeal appeal WHERE appeal.appealId = :appealId")
    Optional<Appeal> findForUpdateByAppealId(@Param("appealId") UUID appealId);

    @Query("""
            SELECT DISTINCT appeal
            FROM Appeal appeal
            JOIN appeal.entry entry
            JOIN entry.race race
            JOIN race.round raceRound
            LEFT JOIN raceRound.headReferee headReferee
            WHERE EXISTS (
                SELECT raceReferee.raceRefereeId
                FROM RaceReferee raceReferee
                WHERE raceReferee.race = race
                  AND raceReferee.referee.refereeId = :refereeId
            )
               OR headReferee.refereeId = :refereeId
            ORDER BY appeal.submittedAt DESC
            """)
    List<Appeal> findAccessibleByRefereeId(@Param("refereeId") UUID refereeId);
}
