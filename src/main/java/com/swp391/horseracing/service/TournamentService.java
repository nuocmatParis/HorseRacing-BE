package com.swp391.horseracing.service;

import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.request.UpdateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.dto.tournament.response.TournamentRatingConfigResponse;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TournamentService {

    TournamentResponse createTournament(CreateTournamentRequest request);

    TournamentResponse update(UUID id, UpdateTournamentRequest request);

    void delete(UUID id);

    TournamentResponse publish(UUID id);

    TournamentResponse completeReview(UUID id);

    TournamentResponse completeMatching(UUID id);

    TournamentResponse publishSchedule(UUID id);

    TournamentResponse publishResults(UUID id);

    List<TournamentResponse> getAll();

    TournamentResponse getById(UUID id);

    TournamentRatingConfigResponse getDefaultRatingConfig();

    TournamentResponse closeRegistration(UUID id);

    TournamentResponse uploadImage(UUID id, MultipartFile file);

    Map<String, Integer> getDefaultPhaseConfigs(int maxApprovedEntries);
}
