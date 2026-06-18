package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;

import java.util.List;
import java.util.UUID;

public interface TournamentService {

    TournamentResponse create(CreateTournamentRequest request);

    List<TournamentResponse> getAll();

    TournamentResponse getById(UUID id);
}
