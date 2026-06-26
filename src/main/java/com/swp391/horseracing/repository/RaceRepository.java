package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Race;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RaceRepository extends JpaRepository<Race, UUID> {

    List<Race> findByRound_RoundId(UUID roundId);
    boolean existsByName(String name);
}