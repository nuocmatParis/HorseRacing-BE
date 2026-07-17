package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.race_result.request.CreateRaceResultRequest;
import com.swp391.horseracing.dto.race_result.request.UpdateRaceResultRequest;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;

import java.util.List;
import java.util.UUID;

public interface RaceResultService {

    List<RaceResultResponse> createResults(UUID raceId, List<CreateRaceResultRequest> requests);

    List<RaceResultResponse> updateResults(UUID raceId, List<UpdateRaceResultRequest> requests);
    List<RaceResultResponse> finishRaceWithRandomResults(UUID raceId);

    List<RaceResultResponse> getResultsByRaceId(UUID raceId);

    List<RaceResultResponse> getRefereeResultsByRaceId(UUID raceId);
}
