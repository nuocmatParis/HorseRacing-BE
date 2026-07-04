package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.enums.ContractStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JockeyHorseContractRepository extends JpaRepository<JockeyHorseContract, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JockeyHorseContract> findForUpdateByContractId(UUID contractId);

    List<JockeyHorseContract> findByTournamentRegistration_HorseRegistrationIdAndStatus(UUID registrationId, ContractStatus status);

    List<JockeyHorseContract> findByJockeyTournamentRegistration_JockeyTournamentRegIdAndStatus(UUID registrationId, ContractStatus status);

    List<JockeyHorseContract> findByStatusOrderByRequestedAtDesc(ContractStatus status);

    List<JockeyHorseContract> findByTournament_TournamentId(UUID tournamentId);

    List<JockeyHorseContract> findByTournament_TournamentIdAndStatus(UUID tournamentId, ContractStatus status);

    Optional<JockeyHorseContract> findByTournamentRegistration_HorseRegistrationId(UUID tournamentRegId);

    boolean existsByTournament_TournamentIdAndHorse_HorseId(UUID tournamentId, UUID horseId);

    List<JockeyHorseContract> findByOwner_OwnerId(UUID ownerId);

    List<JockeyHorseContract> findByJockey_JockeyId(UUID jockeyId);
}
