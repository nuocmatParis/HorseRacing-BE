package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreateRaceRequest;
import com.swp391.horseracing.dto.tournament.response.RaceResponse;

import java.util.UUID;

public interface RaceService {

    RaceResponse create(UUID roundId, CreateRaceRequest request);
}
