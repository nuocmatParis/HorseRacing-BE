package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT DISTINCT r FROM Race r
            WHERE r.schedulePublishedAt IS NOT NULL
              AND r.status = com.swp391.horseracing.enums.RoundStatus.SCHEDULED
              AND r.startTime >= :now
              AND EXISTS (
                  SELECT e.entryId FROM RaceEntry e
                  WHERE e.race = r AND e.contract.owner.user.userId = :userId
              )
            ORDER BY r.startTime ASC
            """)
    Page<Race> findUpcomingForOwner(@Param("userId") UUID userId,
                                    @Param("now") LocalDateTime now,
                                    Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM Race r
            WHERE r.schedulePublishedAt IS NOT NULL
              AND r.status = com.swp391.horseracing.enums.RoundStatus.SCHEDULED
              AND r.startTime >= :now
              AND EXISTS (
                  SELECT e.entryId FROM RaceEntry e
                  WHERE e.race = r AND e.contract.jockey.user.userId = :userId
              )
            ORDER BY r.startTime ASC
            """)
    Page<Race> findUpcomingForJockey(@Param("userId") UUID userId,
                                     @Param("now") LocalDateTime now,
                                     Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM Race r
            WHERE EXISTS (
                  SELECT rr.reportId FROM RaceReport rr
                  WHERE rr.race = r AND rr.status = com.swp391.horseracing.enums.ReportStatus.Published
              )
              AND EXISTS (
                  SELECT e.entryId FROM RaceEntry e
                  WHERE e.race = r AND e.contract.owner.user.userId = :userId
              )
            ORDER BY r.startTime DESC
            """)
    Page<Race> findPublishedResultsForOwner(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM Race r
            WHERE EXISTS (
                  SELECT rr.reportId FROM RaceReport rr
                  WHERE rr.race = r AND rr.status = com.swp391.horseracing.enums.ReportStatus.Published
              )
              AND EXISTS (
                  SELECT e.entryId FROM RaceEntry e
                  WHERE e.race = r AND e.contract.jockey.user.userId = :userId
              )
            ORDER BY r.startTime DESC
            """)
    Page<Race> findPublishedResultsForJockey(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT r FROM Race r
            WHERE r.schedulePublishedAt IS NOT NULL
              AND r.status = com.swp391.horseracing.enums.RoundStatus.SCHEDULED
              AND r.startTime >= :fromTime
              AND (:toTime IS NULL OR r.startTime <= :toTime)
              AND (:tournamentId IS NULL OR r.round.tournament.tournamentId = :tournamentId)
            ORDER BY r.startTime ASC
            """)
    Page<Race> findUpcomingForSpectator(@Param("fromTime") LocalDateTime fromTime,
                                        @Param("toTime") LocalDateTime toTime,
                                        @Param("tournamentId") UUID tournamentId,
                                        Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM Race r
            LEFT JOIN r.round.headReferee headReferee
            WHERE (headReferee.refereeId = :refereeId
                OR EXISTS (
                    SELECT rr.raceRefereeId FROM RaceReferee rr
                    WHERE rr.race = r AND rr.referee.refereeId = :refereeId
                ))
              AND ((r.status = com.swp391.horseracing.enums.RoundStatus.SCHEDULED AND r.startTime >= :now)
                OR r.status = com.swp391.horseracing.enums.RoundStatus.ONGOING
                OR (r.status = com.swp391.horseracing.enums.RoundStatus.CANCELLED AND r.startTime >= :now))
            ORDER BY r.startTime ASC
            """)
    Page<Race> findCurrentForReferee(@Param("refereeId") UUID refereeId,
                                     @Param("now") LocalDateTime now,
                                     Pageable pageable);
}
