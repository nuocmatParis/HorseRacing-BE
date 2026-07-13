package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreateRaceRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateRaceRequest;
import com.swp391.horseracing.dto.tournament.response.RaceResponse;

import java.util.List;
import java.util.UUID;

public interface RaceService {

    RaceResponse create(UUID roundId, CreateRaceRequest request);
    RaceResponse update(UUID raceId, UpdateRaceRequest request);
    void delete(UUID raceId);
    List<RaceResponse> getRacesByRoundId(UUID roundId);
    RaceResponse publishSchedule(UUID raceId);
    RaceResponse startRace(UUID raceId);
    void finalizeRaceEntries(UUID raceId);
    RaceResponse rescheduleRace(UUID raceId, com.swp391.horseracing.dto.tournament.request.RescheduleRaceRequest request);
    void cancelRace(UUID raceId, com.swp391.horseracing.dto.tournament.request.CancelRaceRequest request);
    List<java.time.LocalDateTime> getRescheduleProposals(UUID raceId);
}
