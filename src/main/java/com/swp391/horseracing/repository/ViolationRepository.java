package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ViolationRepository    extends JpaRepository<Violation, UUID> {

    List<Violation> findByRaceEntry_Race_RaceIdOrderByCreatedAtDesc(UUID raceId);
}
