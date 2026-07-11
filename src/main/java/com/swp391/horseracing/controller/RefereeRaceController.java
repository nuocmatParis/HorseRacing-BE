package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.tournament.response.RaceResponse;
import com.swp391.horseracing.service.RaceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/referee/races")
@PreAuthorize("hasRole('REFEREE')")
public class RefereeRaceController {

    RaceService raceService;

    @PostMapping("/{raceId}/start")
    public ApiResponse<RaceResponse> startRace(@PathVariable UUID raceId) {
        return ApiResponse.<RaceResponse>builder()
                .result(raceService.startRace(raceId))
                .build();
    }
}
