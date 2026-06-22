package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.registration.response.JockeyTournamentRegistrationResponse;
import com.swp391.horseracing.service.TournamentRegistrationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
}
