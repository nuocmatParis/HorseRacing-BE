package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.enums.ContractStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JockeyHorseContractRepository extends JpaRepository<JockeyHorseContract, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JockeyHorseContract> findForUpdateByContractId(UUID contractId);

    List<JockeyHorseContract> findByJockey_User_UserIdAndStatusOrderByRequestedAtDesc(UUID userId, ContractStatus status);

    List<JockeyHorseContract> findByStatusOrderByRequestedAtDesc(ContractStatus contractStatus);

    List<JockeyHorseContract> findByHorseTournamentRegistration_HorseRegistrationIdAndStatus(UUID registrationId, ContractStatus status);

    List<JockeyHorseContract> findByJockeyTournamentRegistration_JockeyTournamentRegIdAndStatus(UUID registrationId, ContractStatus status);

    boolean existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
            UUID jockeyTournamentRegId,
            UUID horseTournamentRegId,
            Collection<ContractStatus> statuses
    );

    List<JockeyHorseContract> findByOwner_User_UserIdOrderByRequestedAtDesc(UUID userId);

    List<JockeyHorseContract> findByJockey_User_UserIdOrderByRequestedAtDesc(UUID userId);

    List<JockeyHorseContract> findByTournament_TournamentIdAndStatusAndEscrowStatus(
            UUID tournamentId,
            com.swp391.horseracing.enums.ContractStatus status,
            com.swp391.horseracing.enums.EscrowStatus escrowStatus
    );
}
