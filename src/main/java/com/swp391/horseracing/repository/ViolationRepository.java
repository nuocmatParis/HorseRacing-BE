package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, UUID> {

    List<Violation> findByEntry_EntryId(UUID entryId);

    List<Violation> findByEntry_Race_RaceId(UUID raceId);
}
