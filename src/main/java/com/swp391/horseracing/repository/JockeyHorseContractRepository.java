package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.enums.ContractStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    boolean existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndStatusIn(
            UUID jockeyTournamentRegId,
            Collection<ContractStatus> statuses
    );

    boolean existsByHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
            UUID horseTournamentRegId,
            Collection<ContractStatus> statuses
    );

    boolean existsByJockeyTournamentRegistration_JockeyTournamentRegIdAndContractIdNotAndStatusIn(
            UUID jockeyTournamentRegId,
            UUID excludedContractId,
            Collection<ContractStatus> statuses
    );

    boolean existsByHorseTournamentRegistration_HorseRegistrationIdAndContractIdNotAndStatusIn(
            UUID horseTournamentRegId,
            UUID excludedContractId,
            Collection<ContractStatus> statuses
    );

    List<JockeyHorseContract> findByOwner_User_UserIdOrderByRequestedAtDesc(UUID userId);

    List<JockeyHorseContract> findByJockey_User_UserIdOrderByRequestedAtDesc(UUID userId);

    Page<JockeyHorseContract> findByStatusOrderByRequestedAtDesc(ContractStatus status, Pageable pageable);

    Page<JockeyHorseContract> findByTournament_TournamentIdAndStatusOrderByRequestedAtDesc(
            UUID tournamentId, ContractStatus status, Pageable pageable);

    List<JockeyHorseContract> findByHorseTournamentRegistration_HorseRegistrationIdAndStatusIn(
            UUID registrationId, Collection<ContractStatus> statuses);

    List<JockeyHorseContract> findByTournament_TournamentIdAndStatus(UUID tournamentId, ContractStatus status);

    long countByTournament_TournamentIdAndStatus(UUID tournamentId, ContractStatus status);

    long countByStatus(ContractStatus status);

    List<JockeyHorseContract> findByTournament_TournamentIdAndStatusAndEscrowStatus(
            UUID tournamentId,
            com.swp391.horseracing.enums.ContractStatus status,
            com.swp391.horseracing.enums.EscrowStatus escrowStatus
    );

    @Query("SELECT DISTINCT contract FROM JockeyHorseContract contract "
            + "JOIN FETCH contract.tournament tournament "
            + "JOIN FETCH contract.horse horse "
            + "JOIN FETCH contract.jockey jockey "
            + "JOIN FETCH jockey.user jockeyUser "
            + "JOIN FETCH contract.owner owner "
            + "JOIN FETCH owner.user ownerUser "
            + "WHERE contract.contractId IN :contractIds")
    List<JockeyHorseContract> findAllWithTransactionContextByContractIdIn(
            @Param("contractIds") Collection<UUID> contractIds);
}
