package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.tournament.response.JockeyTournamentRegistrationResponse;
import com.swp391.horseracing.entity.JockeyTournamentRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JockeyTournamentRegistrationMapper {

    @Mapping(target = "tournamentId", source = "tournament.tournamentId")
    @Mapping(target = "tournamentName", source = "tournament.name")
    @Mapping(target = "jockeyId", source = "jockey.jockeyId")
    @Mapping(target = "jockeyName", source = "jockey.user.fullName")
    @Mapping(target = "reviewedById", source = "reviewedBy.userId")
    JockeyTournamentRegistrationResponse toJockeyTournamentRegistrationResponse(JockeyTournamentRegistration registration);
}
