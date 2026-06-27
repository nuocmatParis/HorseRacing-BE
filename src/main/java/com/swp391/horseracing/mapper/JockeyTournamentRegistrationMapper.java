package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.registration.response.JockeyTournamentRegistrationResponse;
import com.swp391.horseracing.entity.JockeyTournamentRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JockeyTournamentRegistrationMapper {

    @Mapping(target = "tournamentId", source = "tournament.tournamentId")
    @Mapping(target = "tournamentName", source = "tournament.name")
    @Mapping(target = "jockeyId", source = "jockey.jockeyId")
    @Mapping(target = "jockeyName", source = "jockey.user.fullName")
    @Mapping(target = "height", source = "jockey.height")
    @Mapping(target = "weight", source = "jockey.weight")
    @Mapping(target = "experienceYears", source = "jockey.experienceYears")
    @Mapping(target = "licenseNumber", source = "jockey.licenseNumber")
    @Mapping(target = "specialization", source = "jockey.specialization")
    @Mapping(target = "hireFee", source = "jockey.hireFee")
    @Mapping(target = "jockeyStatus", source = "jockey.status")
    @Mapping(target = "email", source = "jockey.user.email")
    @Mapping(target = "phoneNumber", source = "jockey.user.phoneNumber")
    @Mapping(target = "reviewedById", source = "reviewedBy.userId")
    @Mapping(target = "reviewedByName", source = "reviewedBy.fullName")
    JockeyTournamentRegistrationResponse toJockeyTournamentRegistrationResponse(JockeyTournamentRegistration registration);
}
