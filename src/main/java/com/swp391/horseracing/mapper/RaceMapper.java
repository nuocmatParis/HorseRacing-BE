package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.tournament.request.CreateRaceRequest;
import com.swp391.horseracing.dto.tournament.response.RaceResponse;
import com.swp391.horseracing.entity.Race;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RaceMapper {

    @Mapping(target = "roundId", source = "round.roundId")
    @Mapping(target = "createdById", source = "createdBy.userId")
    RaceResponse toRaceResponse(Race race);

    @Mapping(target = "raceId", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "finishedAt", ignore = true)
    @Mapping(target = "round", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "startedBy", ignore = true)
    Race toRace(CreateRaceRequest request);
}
