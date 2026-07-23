package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.race_result.request.CreateRaceResultRequest;
import com.swp391.horseracing.dto.race_result.request.UpdateRaceResultRequest;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.service.RaceResultService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RaceResultController {

    RaceResultService raceResultService;

    @PostMapping("/api/referee/races/{raceId}/results")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<List<RaceResultResponse>> createResults(
            @PathVariable UUID raceId,
            @RequestBody @Valid List<CreateRaceResultRequest> requests) {
        return ApiResponse.<List<RaceResultResponse>>builder()
                .result(raceResultService.createResults(raceId, requests))
                .build();
    }

    @PutMapping("/api/referee/races/{raceId}/results")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<List<RaceResultResponse>> updateResults(
            @PathVariable UUID raceId,
            @RequestBody @Valid List<UpdateRaceResultRequest> requests) {
        return ApiResponse.<List<RaceResultResponse>>builder()
                .result(raceResultService.updateResults(raceId, requests))
                .build();
    }

    @GetMapping("/api/races/{raceId}/results")
    public ApiResponse<List<RaceResultResponse>> getResultsByRace(@PathVariable UUID raceId) {
        return ApiResponse.<List<RaceResultResponse>>builder()
                .result(raceResultService.getResultsByRaceId(raceId))
                .build();
    }

    @GetMapping("/api/referee/races/{raceId}/results")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<List<RaceResultResponse>> getRefereeResults(@PathVariable UUID raceId) {
        return ApiResponse.<List<RaceResultResponse>>builder()
                .result(raceResultService.getRefereeResultsByRaceId(raceId))
                .build();
    }

    @GetMapping("/api/head-referee/races/{raceId}/results")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<List<RaceResultResponse>> getHeadRefereeResults(@PathVariable UUID raceId) {
        return ApiResponse.<List<RaceResultResponse>>builder()
                .result(raceResultService.getHeadRefereeResultsByRaceId(raceId))
                .build();
    }

    @PutMapping("/api/head-referee/races/{raceId}/results")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<List<RaceResultResponse>> updateHeadRefereeResults(
            @PathVariable UUID raceId,
            @RequestBody @Valid List<UpdateRaceResultRequest> requests) {
        return ApiResponse.<List<RaceResultResponse>>builder()
                .result(raceResultService.updateHeadRefereeResults(raceId, requests))
                .build();
    }
}
