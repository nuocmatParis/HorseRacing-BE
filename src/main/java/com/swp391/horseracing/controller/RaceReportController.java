package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.race_report.response.RaceReportResponse;
import com.swp391.horseracing.dto.race_report.request.UpdateRaceReportRequest;
import com.swp391.horseracing.dto.race_report.request.ReturnRaceReportRequest;
import com.swp391.horseracing.dto.race_result.response.RaceResultResponse;
import com.swp391.horseracing.service.RaceReportService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;

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

    @PutMapping("/api/referee/races/{raceId}/report")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<RaceReportResponse> updateRefereeReport(
            @PathVariable UUID raceId,
            @RequestBody @Valid UpdateRaceReportRequest request) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.updateRefereeReport(raceId, request))
                .build();
    }

    @PostMapping("/api/referee/races/{raceId}/report/submit")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<RaceReportResponse> submitReport(@PathVariable UUID raceId) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.submitReport(raceId))
                .build();
    }

    @GetMapping("/api/head-referee/rounds/{roundId}/reports")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<List<RaceReportResponse>> getHeadRefereeReports(
            @PathVariable UUID roundId,
            @RequestParam(defaultValue = "SUBMITTED_TO_HEAD") String status) {
        return ApiResponse.<List<RaceReportResponse>>builder()
                .result(raceReportService.getHeadRefereeReports(roundId, status))
                .build();
    }

    @GetMapping("/api/head-referee/races/{raceId}/report")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<RaceReportResponse> getHeadRefereeReport(@PathVariable UUID raceId) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.getHeadRefereeReport(raceId))
                .build();
    }

    @PutMapping("/api/head-referee/races/{raceId}/report")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<RaceReportResponse> updateHeadRefereeReport(
            @PathVariable UUID raceId,
            @RequestBody @Valid UpdateRaceReportRequest request) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.updateHeadRefereeReport(raceId, request))
                .build();
    }

    @PostMapping("/api/head-referee/races/{raceId}/report/return")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<RaceReportResponse> returnReport(
            @PathVariable UUID raceId,
            @RequestBody @Valid ReturnRaceReportRequest request) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.returnReport(raceId, request))
                .build();
    }

    @PostMapping("/api/head-referee/races/{raceId}/report/sign")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<RaceReportResponse> signReport(@PathVariable UUID raceId) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.signReport(raceId))
                .build();
    }

    @PostMapping("/api/referee/races/{raceId}/report/sign")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<RaceReportResponse> signReportLegacy(
            @PathVariable UUID raceId,
            @RequestBody(required = false) UUID ignoredRefereeId) {
        return ApiResponse.<RaceReportResponse>builder()
                .result(raceReportService.signReport(raceId))
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
