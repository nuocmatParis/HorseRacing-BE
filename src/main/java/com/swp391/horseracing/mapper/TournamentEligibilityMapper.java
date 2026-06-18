package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.tournament.request.CreateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentEligibilityResponse;
import com.swp391.horseracing.entity.TournamentEligibility;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TournamentEligibilityMapper {

    @Mapping(target = "tournamentId", source = "tournament.tournamentId")
    TournamentEligibilityResponse toEligibilityResponse(TournamentEligibility eligibility);

    @Mapping(target = "eligibilityId", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    @Mapping(target = "isActive", source = "isActive")
    TournamentEligibility toEligibility(CreateEligibilityRequest request);
}
