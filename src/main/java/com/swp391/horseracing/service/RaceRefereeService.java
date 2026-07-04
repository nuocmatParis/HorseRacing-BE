package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.race_referee.request.CreateRaceRefereeRequest;
import com.swp391.horseracing.dto.race_referee.response.RaceRefereeResponse;

import java.util.List;
import java.util.UUID;

public interface RaceRefereeService {

    RaceRefereeResponse create(CreateRaceRefereeRequest request);

    List<RaceRefereeResponse> getRefereesByRaceId(UUID raceId);

    RaceRefereeResponse getRefereeById(UUID raceRefereeId);

    void delete(UUID raceRefereeId);

    void deleteByRaceAndReferee(UUID raceId, UUID refereeId);
}
