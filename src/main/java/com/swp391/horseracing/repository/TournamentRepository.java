package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.enums.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    List<Tournament> findAllByOrderByCreatedAtDesc();

    List<Tournament> findByStatus(TournamentStatus status);
    boolean existsByName(String name);
}
