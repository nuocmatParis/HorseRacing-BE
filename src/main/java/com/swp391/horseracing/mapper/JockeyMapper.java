package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.jockey.request.JockeyCreationRequest;
import com.swp391.horseracing.dto.jockey.request.JockeyUpdateRequest;
import com.swp391.horseracing.dto.jockey.response.JockeyResponse;
import com.swp391.horseracing.entity.Jockey;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface JockeyMapper {

    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "fullName", source = "user.fullName")
    JockeyResponse toJockeyResponse(Jockey jockey);

    @Mapping(target = "jockeyId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "totalRaces", ignore = true)
    @Mapping(target = "totalWins", ignore = true)
    @Mapping(target = "jockeyTier", ignore = true)
    @Mapping(target = "tierUpdatedAt", ignore = true)
    @Mapping(target = "lastRaceAt", ignore = true)
    @Mapping(target = "tournamentRegistrations", ignore = true)
    Jockey toJockey(JockeyCreationRequest request);

    @Mapping(target = "jockeyId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "totalRaces", ignore = true)
    @Mapping(target = "totalWins", ignore = true)
    @Mapping(target = "jockeyTier", ignore = true)
    @Mapping(target = "tierUpdatedAt", ignore = true)
    @Mapping(target = "lastRaceAt", ignore = true)
    @Mapping(target = "tournamentRegistrations", ignore = true)
    @Mapping(target = "experienceYears", ignore = true)
    void updateJockey(@MappingTarget Jockey jockey, JockeyUpdateRequest request);
}
