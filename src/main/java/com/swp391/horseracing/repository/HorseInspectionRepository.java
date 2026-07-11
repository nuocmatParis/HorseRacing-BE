package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.HorseInspection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HorseInspectionRepository extends JpaRepository<HorseInspection, UUID> {

    Optional<HorseInspection> findByRaceEntry_EntryId(UUID entryId);

    boolean existsByRaceEntry_EntryId(UUID entryId);
}
