package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.PrizeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrizeStructureRepository extends JpaRepository<PrizeStructure, UUID> {

    List<PrizeStructure> findByTournament_TournamentId(UUID tournamentId);
}
