package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.contract.request.CreateContractRequest;
import com.swp391.horseracing.dto.contract.response.ContractResponse;
import com.swp391.horseracing.entity.JockeyHorseContract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {
    @Mapping(source = "tournament.tournamentId", target = "tournamentId")
    @Mapping(source = "tournament.name", target = "tournamentName")
    @Mapping(source = "horseTournamentRegistration.horseRegistrationId", target = "horseTournamentRegId")
    @Mapping(source = "jockeyTournamentRegistration.jockeyTournamentRegId", target = "jockeyTournamentRegId")
    @Mapping(source = "owner.ownerId", target = "ownerId")
    @Mapping(source = "owner.user.fullName", target = "ownerName")
    @Mapping(source = "horse.horseId", target = "horseId")
    @Mapping(source = "horse.horseName", target = "name")
    @Mapping(source = "jockey.jockeyId", target = "jockeyId")
    @Mapping(source = "jockey.user.fullName", target = "jockeyName")
    @Mapping(source = "reviewedBy.userId", target = "reviewedById")
    @Mapping(source = "reviewedBy.fullName", target = "reviewedByName")
    ContractResponse toContractResponse(JockeyHorseContract contract);
}
