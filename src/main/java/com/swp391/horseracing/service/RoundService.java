package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreateRoundRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateRoundRequest;
import com.swp391.horseracing.dto.tournament.response.RoundResponse;

import java.util.List;
import java.util.UUID;

public interface RoundService {
    RoundResponse create(UUID tournamentId, CreateRoundRequest request);
    RoundResponse update(UUID roundId, UpdateRoundRequest request);
    void delete(UUID roundId);
    List<RoundResponse> getRoundsByTournamentId(UUID tournamentId);
    RoundResponse assignHeadReferee(UUID roundId, UUID refereeId);
    RoundResponse removeHeadReferee(UUID roundId);
}
