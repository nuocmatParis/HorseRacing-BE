package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreatePrizeStructureRequest;
import com.swp391.horseracing.dto.tournament.request.UpdatePrizeStructureRequest;
import com.swp391.horseracing.dto.tournament.response.PrizeStructureResponse;

import java.util.List;
import java.util.UUID;

public interface PrizeStructureService {

    PrizeStructureResponse create(UUID tournamentId, CreatePrizeStructureRequest request);
    PrizeStructureResponse update(UUID prizeStructureId, UpdatePrizeStructureRequest request);
    void delete(UUID prizeStructureId);
    List<PrizeStructureResponse> getByTournament(UUID tournamentId);
}
