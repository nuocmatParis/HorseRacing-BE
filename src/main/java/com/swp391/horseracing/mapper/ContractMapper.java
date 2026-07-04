package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.contract.request.CreateContractRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.entity.JockeyHorseContract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    @Mapping(target = "tournamentId", source = "tournament.tournamentId")
    @Mapping(target = "tournamentRegId", source = "tournamentRegistration.horseRegistrationId")
    @Mapping(target = "jockeyTournamentRegId", source = "jockeyTournamentRegistration.jockeyTournamentRegId")
    @Mapping(target = "ownerId", source = "owner.ownerId")
    @Mapping(target = "horseId", source = "horse.horseId")
    @Mapping(target = "jockeyId", source = "jockey.jockeyId")
    @Mapping(target = "advancePayoutStatus", source = "payoutStatus")
    @Mapping(target = "finalPayoutStatus", source = "finalPayoutStatus")
    @Mapping(target = "reviewedById", source = "reviewedBy.userId")
    ContractResponse toContractResponse(JockeyHorseContract contract);

    @Mapping(target = "contractId", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    @Mapping(target = "tournamentRegistration", ignore = true)
    @Mapping(target = "jockeyTournamentRegistration", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "horse", ignore = true)
    @Mapping(target = "jockey", ignore = true)
    @Mapping(target = "advancePaidAmount", ignore = true)
    @Mapping(target = "escrowAmount", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "escrowStatus", ignore = true)
    @Mapping(target = "payoutStatus", ignore = true)
    @Mapping(target = "finalPayoutStatus", ignore = true)
    @Mapping(target = "advancePayoutAt", ignore = true)
    @Mapping(target = "finalPayoutAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "requestedAt", ignore = true)
    @Mapping(target = "respondedAt", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "rejectedReason", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelReason", ignore = true)
    @Mapping(target = "terminatedAt", ignore = true)
    @Mapping(target = "contractNote", ignore = true)
    @Mapping(target = "raceEntries", ignore = true)
    JockeyHorseContract toContract(CreateContractRequest request);
}
