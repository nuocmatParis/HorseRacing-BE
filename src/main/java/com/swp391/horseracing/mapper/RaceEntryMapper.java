package com.swp391.horseracing.mapper;

import com.swp391.horseracing.dto.race_entry.request.CreateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.response.RaceEntryResponse;
import com.swp391.horseracing.entity.RaceEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RaceEntryMapper {

    @Mapping(target = "raceId", source = "race.raceId")
    @Mapping(target = "contractId", source = "contract.contractId")
    @Mapping(target = "assignedById", source = "assignedBy.userId")
    RaceEntryResponse toRaceEntryResponse(RaceEntry raceEntry);

    @Mapping(target = "entryId", ignore = true)
    @Mapping(target = "race", ignore = true)
    @Mapping(target = "contract", ignore = true)
    @Mapping(target = "assignedBy", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "withdrawnAt", ignore = true)
    @Mapping(target = "withdrawReason", ignore = true)
    @Mapping(target = "scratchedReason", ignore = true)
    @Mapping(target = "disqualifiedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RaceEntry toRaceEntry(CreateRaceEntryRequest request);
}
