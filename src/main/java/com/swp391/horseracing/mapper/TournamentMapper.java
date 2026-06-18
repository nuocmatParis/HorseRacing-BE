package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.entity.Tournament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TournamentMapper {

    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    TournamentResponse toTournamentResponse(Tournament tournament);

    @Mapping(target = "tournamentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "phase", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "finishedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "prizeStructures", ignore = true)
    @Mapping(target = "eligibilityRules", ignore = true)
    @Mapping(target = "rounds", ignore = true)
    @Mapping(target = "horseRegistrations", ignore = true)
    @Mapping(target = "jockeyRegistrations", ignore = true)
    Tournament toTournament(CreateTournamentRequest request);
}
