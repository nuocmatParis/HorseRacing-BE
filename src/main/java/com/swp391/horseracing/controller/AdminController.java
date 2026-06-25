package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.registration.response.*;
import com.swp391.horseracing.dto.tournament.request.*;
import com.swp391.horseracing.dto.tournament.response.*;
import com.swp391.horseracing.service.*;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    TournamentEligibilityService tournamentEligibilityService;
    TournamentRegistrationService tournamentRegistrationService;

    @PostMapping("/tournaments")
    public ApiResponse<TournamentResponse> createTournament(@RequestBody @Valid CreateTournamentRequest request) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.createTournament(request))
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

    @PostMapping("/tournaments/{id}/eligibility")
    public ApiResponse<TournamentEligibilityResponse> createEligibility(@PathVariable UUID id,
                                                                         @RequestBody @Valid CreateEligibilityRequest request) {
        return ApiResponse.<TournamentEligibilityResponse>builder()
                .result(tournamentEligibilityService.create(id, request))
                .build();
    }
    @PostMapping("/tournaments/{id}/publish")
    public ApiResponse<TournamentResponse> publishTournament(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.publish(id))
                .build();
    }

    @GetMapping("/horse-registrations")
    public ApiResponse<List<HorseTournamentRegistrationResponse>> getAllHorseRegistrations(){
    return ApiResponse.<List<HorseTournamentRegistrationResponse>>builder()
            .result(tournamentRegistrationService.getAllHorseRegistrations())
            .build();
    }

    @GetMapping("/jockey-registrations")
    public ApiResponse<List<JockeyTournamentRegistrationResponse>> getAllJockeyRegistrations(){
        return ApiResponse.<List<JockeyTournamentRegistrationResponse>>builder()
                .result(tournamentRegistrationService.getAllJockeyRegistrations())
                .build();
    }

    @PostMapping("/tournaments/{id}/complete-review")
    public ApiResponse<TournamentResponse> completeReview(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.completeReview(id))
                .build();
    }

    @PostMapping("/tournaments/{id}/complete-matching")
    public ApiResponse<TournamentResponse> completeMatching(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.completeMatching(id))
                .build();
    }

    @PostMapping("/tournaments/{id}/publish-schedule")
    public ApiResponse<TournamentResponse> publishSchedule(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.publishSchedule(id))
                .build();
    }

    @PostMapping("/tournaments/{id}/publish-results")
    public ApiResponse<TournamentResponse> publishResults(@PathVariable UUID id) {
        return ApiResponse.<TournamentResponse>builder()
                .result(tournamentService.publishResults(id))
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
