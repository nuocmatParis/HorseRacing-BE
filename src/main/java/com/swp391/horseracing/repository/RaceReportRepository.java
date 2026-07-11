package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.RaceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RaceReportRepository extends JpaRepository<RaceReport, UUID> {

    Optional<RaceReport> findByRace_RaceId(UUID raceId);

    boolean existsByRace_RaceId(UUID raceId);
}
