package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.entity.Tournament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TournamentMapper {

    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    TournamentResponse toTournamentResponse(Tournament tournament);

    @Mapping(target = "createdBy", ignore = true)
    Tournament toTournament(CreateTournamentRequest request);
}
