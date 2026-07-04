package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.horse.request.HorseCreationRequest;
import com.swp391.horseracing.dto.horse.request.HorseUpdateRequest;
import com.swp391.horseracing.dto.horse.response.HorseResponse;
import com.swp391.horseracing.entity.Horse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface HorseMapper {

    @Mapping(target = "ownerId", source = "owner.ownerId")
    HorseResponse toHorseResponse(Horse horse);

    @Mapping(target = "horseId", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "currentRating", ignore = true)
    @Mapping(target = "highestRating", ignore = true)
    @Mapping(target = "ratingUpdatedAt", ignore = true)
    @Mapping(target = "totalRaces", ignore = true)
    @Mapping(target = "totalWins", ignore = true)
    @Mapping(target = "totalTop3Finishes", ignore = true)
    @Mapping(target = "winRate", ignore = true)
    @Mapping(target = "lastRaceAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "tournamentRegistrations", ignore = true)
    Horse toHorse(HorseCreationRequest request);

    @Mapping(target = "horseId", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "currentRating", ignore = true)
    @Mapping(target = "highestRating", ignore = true)
    @Mapping(target = "ratingUpdatedAt", ignore = true)
    @Mapping(target = "totalRaces", ignore = true)
    @Mapping(target = "totalWins", ignore = true)
    @Mapping(target = "totalTop3Finishes", ignore = true)
    @Mapping(target = "winRate", ignore = true)
    @Mapping(target = "lastRaceAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "tournamentRegistrations", ignore = true)
    void updateHorse(@MappingTarget Horse horse, HorseUpdateRequest request);
}
