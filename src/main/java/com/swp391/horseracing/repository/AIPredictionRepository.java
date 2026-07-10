package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.AIPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AIPredictionRepository extends JpaRepository<AIPrediction, UUID> {

    List<AIPrediction> findByEntry_Race_RaceId(UUID raceId);

    Optional<AIPrediction> findByEntry_EntryId(UUID entryId);

    boolean existsByEntry_EntryId(UUID entryId);

    void deleteByEntry_Race_RaceId(UUID raceId);
}
