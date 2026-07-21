package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.registration.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.entity.HorseTournamentRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HorseTournamentRegistrationMapper {

    @Mapping(target = "tournamentId", source = "tournament.tournamentId")
    @Mapping(target = "tournamentName", source = "tournament.name")
    @Mapping(target = "horseId", source = "horse.horseId")
    @Mapping(target = "horseName", source = "horse.name")
    @Mapping(target = "horseBreed", source = "horse.breed")
    @Mapping(target = "horseGender", source = "horse.gender")
    @Mapping(target = "horseAge", source = "horse.age")
    @Mapping(target = "horseWeight", source = "horse.weight")
    @Mapping(target = "horseColor", source = "horse.color")
    @Mapping(target = "horseHealthStatus", source = "horse.healthStatus")
    @Mapping(target = "horseRaceClass", source = "horse.raceClass")
    @Mapping(target = "horseTotalRaces", source = "horse.totalRaces")
    @Mapping(target = "horseTotalWins", source = "horse.totalWins")
    @Mapping(target = "horseWinRate", source = "horse.winRate")
    @Mapping(target = "ownerId", source = "owner.ownerId")
    @Mapping(target = "ownerName", source = "owner.user.fullName")
    @Mapping(target = "farmName", source = "owner.farmName")
    @Mapping(target = "ownerAddress", source = "owner.address")
    @Mapping(target = "reviewedById", source = "reviewedBy.userId")
    @Mapping(target = "reviewedByName", source = "reviewedBy.fullName")
    HorseTournamentRegistrationResponse toHorseTournamentRegistrationResponse(HorseTournamentRegistration registration);
}
