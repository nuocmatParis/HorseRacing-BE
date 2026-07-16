package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.RaceReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import com.swp391.horseracing.enums.ReportStatus;

@Repository
public interface RaceReportRepository extends JpaRepository<RaceReport, UUID> {

    Optional<RaceReport> findByRace_RaceId(UUID raceId);

    boolean existsByRace_RaceId(UUID raceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RaceReport r WHERE r.race.raceId = :raceId")
    Optional<RaceReport> findForUpdateByRace_RaceId(@Param("raceId") UUID raceId);

    List<RaceReport> findByRace_Round_RoundIdAndStatusOrderBySubmittedAtAsc(
            UUID roundId, ReportStatus status);
}
