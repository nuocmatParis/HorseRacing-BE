package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.registration.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.dto.registration.response.JockeyTournamentRegistrationResponse;
import com.swp391.horseracing.service.TournamentRegistrationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jockey")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('JOCKEY')")
public class JockeyTournamentController {

    TournamentRegistrationService tournamentRegistrationService;

    @PostMapping("/tournaments/{id}/register")
    public ApiResponse<JockeyTournamentRegistrationResponse> register(@PathVariable UUID id) {
        return ApiResponse.<JockeyTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.registerJockey(id))
                .build();
    }

    @GetMapping("/tournaments/{id}/accepted-horses")
    public ApiResponse<List<HorseTournamentRegistrationResponse>> getAcceptedHorses(@PathVariable UUID id) {
        return ApiResponse.<List<HorseTournamentRegistrationResponse>>builder()
                .result(tournamentRegistrationService.getApprovedHorsesByTournament(id))
                .build();
    }

    @GetMapping("/my-registrations")
    public ApiResponse<List<JockeyTournamentRegistrationResponse>> getMyRegistrations(){
        return ApiResponse.<List<JockeyTournamentRegistrationResponse>>builder()
                .result(tournamentRegistrationService.getMyJockeyRegistrations())
                .build();
    }
}
