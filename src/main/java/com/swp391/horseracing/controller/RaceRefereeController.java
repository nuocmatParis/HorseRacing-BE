package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.race_referee.request.CreateRaceRefereeRequest;
import com.swp391.horseracing.dto.race_referee.response.RaceRefereeResponse;
import com.swp391.horseracing.service.RaceRefereeService;
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
@RequestMapping("/api/race-referees")
public class RaceRefereeController {

    RaceRefereeService raceRefereeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RaceRefereeResponse> create(@RequestBody @Valid CreateRaceRefereeRequest request) {
        return ApiResponse.<RaceRefereeResponse>builder()
                .result(raceRefereeService.create(request))
                .build();
    }

    @GetMapping("/race/{raceId}")
    public ApiResponse<List<RaceRefereeResponse>> getRefereesByRace(@PathVariable UUID raceId) {
        return ApiResponse.<List<RaceRefereeResponse>>builder()
                .result(raceRefereeService.getRefereesByRaceId(raceId))
                .build();
    }

    @GetMapping("/{raceRefereeId}")
    public ApiResponse<RaceRefereeResponse> getRefereeById(@PathVariable UUID raceRefereeId) {
        return ApiResponse.<RaceRefereeResponse>builder()
                .result(raceRefereeService.getRefereeById(raceRefereeId))
                .build();
    }

    @DeleteMapping("/{raceRefereeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID raceRefereeId) {
        raceRefereeService.delete(raceRefereeId);
        return ApiResponse.<Void>builder().build();
    }
}
