package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.entity.RaceResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RaceResultMapper {

    @Mapping(target = "raceId", source = "race.raceId")
    @Mapping(target = "entryId", source = "entry.entryId")
    @Mapping(target = "recordedById", source = "recordedBy.userId")
    RaceResultResponse toRaceResultResponse(RaceResult raceResult);
}
