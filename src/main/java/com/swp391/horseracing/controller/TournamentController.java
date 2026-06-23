package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.tournament.response.PrizeStructureResponse;
import com.swp391.horseracing.dto.tournament.response.TournamentEligibilityResponse;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
import com.swp391.horseracing.service.PrizeStructureService;
import com.swp391.horseracing.service.TournamentEligibilityService;
import com.swp391.horseracing.service.TournamentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TournamentController {

    TournamentService tournamentService;
    PrizeStructureService prizeStructureService;
    TournamentEligibilityService tournamentEligibilityService;

    @GetMapping
    public ApiResponse<List<TournamentResponse>> getAll() {
        return ApiResponse.<List<TournamentResponse>>builder()
                .result(tournamentService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<TournamentResponse> getById(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.getById(id))
                .build();
    }

    @GetMapping("/{id}/prizes")
    public ApiResponse<List<PrizeStructureResponse>> getPrizesByTournament(@PathVariable UUID id) {
        return ApiResponse.<List<PrizeStructureResponse>>builder()
                .result(prizeStructureService.getByTournament(id))
                .build();
    }

    @GetMapping("/{id}/eligibility")
    public ApiResponse<List<TournamentEligibilityResponse>> getEligibilityByTournament(@PathVariable UUID id) {
        return ApiResponse.<List<TournamentEligibilityResponse>>builder()
                .result(tournamentEligibilityService.getByTournament(id))
                .build();
    }
}
