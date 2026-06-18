package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.JockeyTournamentRegistration;
import com.swp391.horseracing.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JockeyTournamentRegistrationRepository extends JpaRepository<JockeyTournamentRegistration, UUID> {

    boolean existsByTournament_TournamentIdAndJockey_JockeyId(UUID tournamentId, UUID jockeyId);

    List<JockeyTournamentRegistration> findByTournament_TournamentId(UUID tournamentId);

    List<JockeyTournamentRegistration> findByJockey_JockeyId(UUID jockeyId);

    List<JockeyTournamentRegistration> findByStatus(RegistrationStatus status);

    Optional<JockeyTournamentRegistration> findByTournament_TournamentIdAndJockey_JockeyId(UUID tournamentId, UUID jockeyId);
}
