package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.HorseTournamentRegistration;
import com.swp391.horseracing.enums.RegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HorseTournamentRegistrationRepository extends JpaRepository<HorseTournamentRegistration, UUID> {

    boolean existsByTournament_TournamentIdAndHorse_HorseId(UUID tournamentId, UUID horseId);

    Optional<HorseTournamentRegistration> findByTournament_TournamentIdAndHorse_HorseId(UUID tournamentId, UUID horseId);

    List<HorseTournamentRegistration> findByOwner_OwnerId(UUID ownerId);

    List<HorseTournamentRegistration> findByTournament_TournamentIdAndStatus(UUID tournamentId, RegistrationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM HorseTournamentRegistration r WHERE r.tournament.tournamentId = :tournamentId AND r.status = :status")
    List<HorseTournamentRegistration> findForUpdateByTournamentIdAndStatus(
            @Param("tournamentId") UUID tournamentId, @Param("status") RegistrationStatus status);

    @Query("SELECT COUNT(r) > 0 FROM HorseTournamentRegistration r " +
            "WHERE r.horse.horseId = :horseId " +
            "AND r.status IN :statuses " +
            "AND r.tournament.competitionStartAt <= :endDateTime " +
            "AND r.tournament.endDate >= :startDate")
    boolean existsHorseWithConflictingTournament(
            @Param("horseId") UUID horseId,
            @Param("startDate") LocalDate startDate,
            @Param("endDateTime") java.time.LocalDateTime endDateTime,
            @Param("statuses") List<RegistrationStatus> statuses);

    long countByTournament_TournamentIdAndStatus(UUID tournamentId, RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM HorseTournamentRegistration r WHERE r.horseRegistrationId = :registrationId")
    Optional<HorseTournamentRegistration> findForUpdateById(@Param("registrationId") UUID registrationId);
}
