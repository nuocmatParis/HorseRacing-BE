package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.horseinspection.response.HorseInspectionResponse;
import com.swp391.horseracing.entity.HorseInspection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HorseInspectionMapper {

    @Mapping(target = "entryId", source = "raceEntry.entryId")
    @Mapping(target = "horseId", source = "raceEntry.contract.horse.horseId")
    @Mapping(target = "horseName", source = "raceEntry.contract.horse.name")
    @Mapping(target = "veterinarianId", source = "veterinarian.vetId")
    @Mapping(target = "veterinarianName", source = "veterinarian.user.fullName")
    @Mapping(target = "handicapConfirmed", source = "isHandicapConfirmed")
    HorseInspectionResponse toResponse(HorseInspection inspection);
}
