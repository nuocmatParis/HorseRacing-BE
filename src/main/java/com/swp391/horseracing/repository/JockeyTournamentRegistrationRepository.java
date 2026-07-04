package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.JockeyTournamentRegistration;
import com.swp391.horseracing.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JockeyTournamentRegistrationRepository extends JpaRepository<JockeyTournamentRegistration, UUID> {

    boolean existsByTournament_TournamentIdAndJockey_JockeyId(UUID tournamentId, UUID jockeyId);

    List<JockeyTournamentRegistration> findByJockey_JockeyId(UUID jockeyId);

    List<JockeyTournamentRegistration> findByStatus(RegistrationStatus status);

    List<JockeyTournamentRegistration> findByTournament_TournamentIdAndStatus(UUID tournamentId, RegistrationStatus status);

    @Query("SELECT COUNT(r) > 0 FROM JockeyTournamentRegistration r " +
            "WHERE r.jockey.jockeyId = :jockeyId " +
            "AND r.status IN :statuses " +
            "AND r.tournament.startDate <= :endDate " +
            "AND r.tournament.endDate >= :startDate")
    boolean existsJockeyWithConflictingTournament(
            @Param("jockeyId") UUID jockeyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<RegistrationStatus> statuses);

    long countByTournament_TournamentIdAndStatus(UUID tournamentId, RegistrationStatus status);
}
