package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.tournament.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.entity.HorseTournamentRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HorseTournamentRegistrationMapper {

    @Mapping(target = "tournamentId", source = "tournament.tournamentId")
    @Mapping(target = "tournamentName", source = "tournament.name")
    @Mapping(target = "horseId", source = "horse.horseId")
    @Mapping(target = "horseName", source = "horse.name")
    @Mapping(target = "reviewedById", source = "reviewedBy.userId")
    HorseTournamentRegistrationResponse toHorseTournamentRegistrationResponse(HorseTournamentRegistration registration);
}
