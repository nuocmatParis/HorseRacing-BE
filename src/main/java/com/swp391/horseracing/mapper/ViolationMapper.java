package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.violation.response.ViolationResponse;
import com.swp391.horseracing.entity.Violation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ViolationMapper {

    @Mapping(target = "raceId", source = "raceEntry.race.raceId")
    @Mapping(target = "entryId", source = "raceEntry.entryId")
    @Mapping(target = "horseId", source = "raceEntry.contract.horse.horseId")
    @Mapping(target = "horseName", source = "raceEntry.contract.horse.name")
    @Mapping(target = "jockeyId", source = "raceEntry.contract.jockey.jockeyId")
    @Mapping(target = "jockeyName", source = "raceEntry.contract.jockey.user.fullName")
    @Mapping(target = "refereeId", source = "referee.refereeId")
    @Mapping(target = "refereeName", source = "referee.user.fullName")
    ViolationResponse toResponse(Violation violation);
}
