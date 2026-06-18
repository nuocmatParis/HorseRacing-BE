package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.tournament.request.RegisterHorseRequest;
import com.swp391.horseracing.dto.tournament.response.HorseTournamentRegistrationResponse;
import com.swp391.horseracing.service.TournamentRegistrationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('HORSE_OWNER')")
public class OwnerTournamentController {

    TournamentRegistrationService tournamentRegistrationService;

    @PostMapping("/tournaments/{id}/register-horse")
    public ApiResponse<HorseTournamentRegistrationResponse> registerHorse(@PathVariable UUID id,
                                                                           @RequestBody @Valid RegisterHorseRequest request) {
        return ApiResponse.<HorseTournamentRegistrationResponse>builder()
                .result(tournamentRegistrationService.registerHorse(id, request.getHorseId()))
                .build();
    }
}
