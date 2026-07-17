package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.tournament.response.RaceResponse;
import com.swp391.horseracing.dto.race.response.RaceStartReadinessResponse;
import com.swp391.horseracing.service.RaceService;
import com.swp391.horseracing.service.RaceResultService;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/referee/races")
@PreAuthorize("hasRole('REFEREE')")
public class RefereeRaceController {

    RaceService raceService;
    RaceResultService raceResultService;

    @PostMapping("/{raceId}/start")
    public ApiResponse<RaceResponse> startRace(@PathVariable UUID raceId) {
        return ApiResponse.<RaceResponse>builder()
                .result(raceService.startRace(raceId))
                .build();
    }

    @PostMapping("/{raceId}/finish")
    public ApiResponse<List<RaceResultResponse>> finishRace(@PathVariable UUID raceId) {
        return ApiResponse.<List<RaceResultResponse>>builder()
                .result(raceResultService.finishRaceWithRandomResults(raceId))
                .build();
    }

    @GetMapping("/{raceId}/start-readiness")
    public ApiResponse<RaceStartReadinessResponse> getStartReadiness(@PathVariable UUID raceId) {
        return ApiResponse.<RaceStartReadinessResponse>builder()
                .result(raceService.getStartReadiness(raceId))
                .build();
    }
}
