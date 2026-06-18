package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HorseTournamentRegistrationRepository extends JpaRepository<HorseTournamentRegistration, UUID> {

    boolean existsByTournament_TournamentIdAndHorse_HorseId(UUID tournamentId, UUID horseId);

    List<HorseTournamentRegistration> findByTournament_TournamentId(UUID tournamentId);

    List<HorseTournamentRegistration> findByOwner_OwnerId(UUID ownerId);

    List<HorseTournamentRegistration> findByStatus(RegistrationStatus status);

    Optional<HorseTournamentRegistration> findByTournament_TournamentIdAndHorse_HorseId(UUID tournamentId, UUID horseId);
}
