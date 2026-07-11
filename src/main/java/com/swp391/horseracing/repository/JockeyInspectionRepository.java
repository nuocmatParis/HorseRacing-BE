package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.JockeyInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JockeyInspectionRepository   extends JpaRepository<JockeyInspection, UUID> {

    Optional<JockeyInspection> findByRaceEntry_EntryId(UUID entryId);

    boolean existsByRaceEntry_EntryId(UUID entryId);
}
