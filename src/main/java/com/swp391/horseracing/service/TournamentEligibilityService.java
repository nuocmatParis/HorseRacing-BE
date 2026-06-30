package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentEligibilityResponse;

import java.util.List;
import java.util.UUID;

public interface TournamentEligibilityService {

    TournamentEligibilityResponse create(UUID tournamentId, CreateEligibilityRequest request);
    TournamentEligibilityResponse update(UUID eligibilityId, UpdateEligibilityRequest request);
    void delete(UUID eligibilityId);
    List<TournamentEligibilityResponse> getByTournament(UUID tournamentId);

}
