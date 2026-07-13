package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.Tournament;
import com.swp391.horseracing.enums.TournamentPhase;
import com.swp391.horseracing.enums.TournamentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {

    List<Tournament> findAllByOrderByCreatedAtDesc();

    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByPhase(TournamentPhase phase);

    boolean existsByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tournament t WHERE t.tournamentId = :tournamentId")
    Optional<Tournament> findForUpdateByTournamentId(@Param("tournamentId") UUID tournamentId);
}
