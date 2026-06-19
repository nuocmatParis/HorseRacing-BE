package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreateEligibilityRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentEligibilityResponse;

import java.util.UUID;

public interface TournamentEligibilityService {

    TournamentEligibilityResponse create(UUID tournamentId, CreateEligibilityRequest request);
}
