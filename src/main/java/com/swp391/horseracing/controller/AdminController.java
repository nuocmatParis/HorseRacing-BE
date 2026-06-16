package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.tournament.request.CreatePrizeStructureRequest;
import com.swp391.horseracing.dto.tournament.request.CreateRaceRequest;
import com.swp391.horseracing.dto.tournament.request.CreateRoundRequest;
import com.swp391.horseracing.dto.tournament.request.CreateTournamentRequest;
import com.swp391.horseracing.dto.tournament.response.*;
import com.swp391.horseracing.service.PrizeStructureService;
import com.swp391.horseracing.service.RaceService;
import com.swp391.horseracing.service.RoundService;
import com.swp391.horseracing.service.TournamentRegistrationService;
import com.swp391.horseracing.service.TournamentService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    TournamentService tournamentService;
    RoundService roundService;
    RaceService raceService;
    PrizeStructureService prizeStructureService;
    TournamentRegistrationService tournamentRegistrationService;

    @PostMapping("/tournaments")
    public ApiResponse<TournamentResponse> createTournament(@RequestBody @Valid CreateTournamentRequest request) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.create(request))
                .build();
    }

    @PostMapping("/tournaments/{id}/rounds")
    public ApiResponse<RoundResponse> createRound(@PathVariable UUID id,
                                                   @RequestBody @Valid CreateRoundRequest request) {
        return ApiResponse.<RoundResponse>builder()
                .result(roundService.create(id, request))
                .build();
    }

    @PostMapping("/rounds/{roundId}/races")
    public ApiResponse<RaceResponse> createRace(@PathVariable UUID roundId,
                                                 @RequestBody @Valid CreateRaceRequest request) {
        return ApiResponse.<RaceResponse>builder()
                .result(raceService.create(roundId, request))
                .build();
    }

    @PostMapping("/tournaments/{id}/prize-structures")
    public ApiResponse<PrizeStructureResponse> createPrizeStructure(@PathVariable UUID id,
                                                                     @RequestBody @Valid CreatePrizeStructureRequest request) {
        return ApiResponse.<PrizeStructureResponse>builder()
                .result(prizeStructureService.create(id, request))
                .build();
    }

    @PostMapping("/horse-registrations/{id}/approve")
    public ApiResponse<HorseTournamentRegistrationResponse> approveHorseRegistration(@PathVariable UUID id) {
        return ApiResponse.<HorseTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.approveHorseRegistration(id))
                .build();
    }

    @PostMapping("/horse-registrations/{id}/reject")
    public ApiResponse<HorseTournamentRegistrationResponse> rejectHorseRegistration(@PathVariable UUID id,
                                                                                     @RequestBody(required = false) String reason) {
        return ApiResponse.<HorseTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.rejectHorseRegistration(id, reason))
                .build();
    }

    @PostMapping("/jockey-registrations/{id}/approve")
    public ApiResponse<JockeyTournamentRegistrationResponse> approveJockeyRegistration(@PathVariable UUID id) {
        return ApiResponse.<JockeyTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.approveJockeyRegistration(id))
                .build();
    }

    @PostMapping("/jockey-registrations/{id}/reject")
    public ApiResponse<JockeyTournamentRegistrationResponse> rejectJockeyRegistration(@PathVariable UUID id,
                                                                                       @RequestBody(required = false) String reason) {
        return ApiResponse.<JockeyTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.rejectJockeyRegistration(id, reason))
                .build();
    }
}
