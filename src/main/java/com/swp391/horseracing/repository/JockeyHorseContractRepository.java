package com.swp391.horseracing.repository;

import com.swp391.horseracing.entity.JockeyHorseContract;
import com.swp391.horseracing.enums.ContractStatus;
import jakarta.persistence.LockModeType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JockeyHorseContractRepository extends JpaRepository<JockeyHorseContract, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JockeyHorseContract> findForUpdateByContractId(UUID contractId);

    List<JockeyHorseContract> findByTournamentRegistration_RegistrationIdAndStatus(UUID registrationId, ContractStatus status);

    List<JockeyHorseContract> findByJockeyTournamentRegistration_jockeyTournamentRegIdAndStatus(UUID registrationId, ContractStatus status);

    List<JockeyHorseContract> findByStatusOrderByRequestedAtDesc(ContractStatus status);

}
