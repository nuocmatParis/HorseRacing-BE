package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.race_portal.RaceSummaryResponse;
import com.swp391.horseracing.dto.race_portal.SpectatorRaceDetailResponse;
import com.swp391.horseracing.service.RacePortalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('SPECTATOR')")
public class SpectatorRaceController {
    RacePortalService racePortalService;

    @GetMapping("/api/spectator/races/upcoming")
    public ApiResponse<PageResponse<RaceSummaryResponse>> getUpcomingRaces(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) UUID tournamentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RaceSummaryResponse>>builder()
                .result(racePortalService.getUpcomingRaces(from, to, tournamentId, page, size))
                .build();
    }

    @GetMapping("/api/spectator/races/{raceId}")
    public ApiResponse<SpectatorRaceDetailResponse> getRaceDetail(@PathVariable UUID raceId) {
        return ApiResponse.<SpectatorRaceDetailResponse>builder()
                .result(racePortalService.getSpectatorRaceDetail(raceId))
                .build();
    }
}
