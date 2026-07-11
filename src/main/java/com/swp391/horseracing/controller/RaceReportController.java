package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.race_report.response.RaceReportResponse;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.service.RaceReportService;
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
public class RaceReportController {

    RaceReportService raceReportService;

    @GetMapping("/api/referee/races/{raceId}/report")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<RaceReportResponse> getRefereeReport(@PathVariable UUID raceId) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.getRefereeReport(raceId))
                .build();
    }

    @PostMapping("/api/referee/races/{raceId}/report/sign")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<RaceReportResponse> signReport(@PathVariable UUID raceId,
                                                       @RequestBody UUID refereeId) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.signReport(raceId, refereeId))
                .build();
    }

    @GetMapping("/api/admin/races/{raceId}/report")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RaceReportResponse> getAdminReport(@PathVariable UUID raceId) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.getAdminReport(raceId))
                .build();
    }

    @PostMapping("/api/admin/races/{raceId}/report/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RaceReportResponse> publishReport(@PathVariable UUID raceId) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.publishReport(raceId))
                .build();
    }

    @GetMapping("/api/races/{raceId}/report")
    public ApiResponse<RaceReportResponse> getPublishedReport(@PathVariable UUID raceId) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.getPublishedReport(raceId))
                .build();
    }

    @GetMapping("/api/races/{raceId}/ranking")
    public ApiResponse<List<RaceResultResponse>> getRaceRanking(@PathVariable UUID raceId) {
        return ApiResponse.<List<RaceResultResponse>>builder()
                .result(raceReportService.getRaceRanking(raceId))
                .build();
    }
}
