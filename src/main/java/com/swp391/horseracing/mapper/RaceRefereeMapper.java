package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.race_referee.request.CreateRaceRefereeRequest;
import com.swp391.horseracing.dto.race_referee.response.RaceRefereeResponse;
import com.swp391.horseracing.entity.RaceReferee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RaceRefereeMapper {

    @Mapping(target = "raceId", source = "race.raceId")
    @Mapping(target = "refereeId", source = "referee.refereeId")
    @Mapping(target = "assignedById", source = "assignedBy.userId")
    RaceRefereeResponse toRaceRefereeResponse(RaceReferee raceReferee);

    @Mapping(target = "raceRefereeId", ignore = true)
    @Mapping(target = "race", ignore = true)
    @Mapping(target = "referee", ignore = true)
    @Mapping(target = "assignedBy", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    RaceReferee toRaceReferee(CreateRaceRefereeRequest request);
}
