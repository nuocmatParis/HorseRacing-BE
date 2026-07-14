package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.entity.RaceResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RaceResultMapper {

    @Mapping(target = "raceId", source = "race.raceId")
    @Mapping(target = "entryId", source = "entry.entryId")
    @Mapping(target = "laneNumber", source = "entry.laneNumber")
    @Mapping(target = "horseId", source = "entry.contract.horse.horseId")
    @Mapping(target = "horseName", source = "entry.contract.horse.name")
    @Mapping(target = "jockeyId", source = "entry.contract.jockey.jockeyId")
    @Mapping(target = "jockeyName", source = "entry.contract.jockey.user.fullName")
    @Mapping(target = "recordedById", source = "recordedBy.userId")
    @Mapping(target = "isPrizePaid", source = "prizePaid")
    RaceResultResponse toRaceResultResponse(RaceResult raceResult);
}
