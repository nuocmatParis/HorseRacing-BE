package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.race_entry.request.CreateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.request.UpdateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.response.RaceEntryResponse;

import java.util.List;
import java.util.UUID;

public interface RaceEntryService {

    RaceEntryResponse create(CreateRaceEntryRequest request);

    RaceEntryResponse updateStatus(UUID entryId, UpdateRaceEntryRequest request);

    List<RaceEntryResponse> getEntriesByRaceId(UUID raceId);

    RaceEntryResponse getEntryById(UUID entryId);

    void delete(UUID entryId);

    void autoAssignRound(UUID roundId);

    void autoAssignLanes(UUID raceId);

    RaceEntryResponse updateLane(UUID entryId, Integer laneNumber);

    RaceEntryResponse swapLanes(UUID entryId1, UUID entryId2);
}
