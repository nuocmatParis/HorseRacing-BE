package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.race_portal.RaceResultsResponse;
import com.swp391.horseracing.dto.race_portal.RaceScheduleResponse;
import com.swp391.horseracing.service.RacePortalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ParticipantRaceController {
    RacePortalService racePortalService;

    @GetMapping("/api/owner/race-schedule")
    @PreAuthorize("hasRole('HORSE_OWNER')")
    public ApiResponse<PageResponse<RaceScheduleResponse>> getOwnerSchedule(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RaceScheduleResponse>>builder()
                .result(racePortalService.getOwnerSchedule(page, size))
                .build();
    }

    @GetMapping("/api/jockey/race-schedule")
    @PreAuthorize("hasRole('JOCKEY')")
    public ApiResponse<PageResponse<RaceScheduleResponse>> getJockeySchedule(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RaceScheduleResponse>>builder()
                .result(racePortalService.getJockeySchedule(page, size))
                .build();
    }

    @GetMapping("/api/owner/races")
    @PreAuthorize("hasRole('HORSE_OWNER')")
    public ApiResponse<PageResponse<RaceScheduleResponse>> getOwnerRaces(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RaceScheduleResponse>>builder()
                .result(racePortalService.getOwnerRaces(from, to, page, size))
                .build();
    }

    @GetMapping("/api/jockey/races")
    @PreAuthorize("hasRole('JOCKEY')")
    public ApiResponse<PageResponse<RaceScheduleResponse>> getJockeyRaces(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RaceScheduleResponse>>builder()
                .result(racePortalService.getJockeyRaces(from, to, page, size))
                .build();
    }

    @GetMapping("/api/owner/race-results")
    @PreAuthorize("hasRole('HORSE_OWNER')")
    public ApiResponse<PageResponse<RaceResultsResponse>> getOwnerResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RaceResultsResponse>>builder()
                .result(racePortalService.getOwnerResults(page, size))
                .build();
    }

    @GetMapping("/api/jockey/race-results")
    @PreAuthorize("hasRole('JOCKEY')")
    public ApiResponse<PageResponse<RaceResultsResponse>> getJockeyResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RaceResultsResponse>>builder()
                .result(racePortalService.getJockeyResults(page, size))
                .build();
    }

    @GetMapping("/api/owner/race-results/provisional")
    @PreAuthorize("hasRole('HORSE_OWNER')")
    public ApiResponse<PageResponse<RaceResultsResponse>> getOwnerProvisionalResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RaceResultsResponse>>builder()
                .result(racePortalService.getOwnerProvisionalResults(page, size))
                .build();
    }

    @GetMapping("/api/jockey/race-results/provisional")
    @PreAuthorize("hasRole('JOCKEY')")
    public ApiResponse<PageResponse<RaceResultsResponse>> getJockeyProvisionalResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<RaceResultsResponse>>builder()
                .result(racePortalService.getJockeyProvisionalResults(page, size))
                .build();
    }
}
