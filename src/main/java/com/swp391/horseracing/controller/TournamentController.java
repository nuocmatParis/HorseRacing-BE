package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.tournament.response.TournamentResponse;
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
}
