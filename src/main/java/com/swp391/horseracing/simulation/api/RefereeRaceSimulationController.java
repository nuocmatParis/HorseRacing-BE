package com.swp391.horseracing.simulation.api;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.simulation.realtime.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/referee/races/{raceId}/simulation")
@PreAuthorize("hasRole('REFEREE')")
@RequiredArgsConstructor
public class RefereeRaceSimulationController {
    private final RaceSimulationLifecycleService lifecycleService;
    private final RaceIncidentService incidentService;
    private final RaceLiveQueryService queryService;
    private final SimulationAccessService accessService;

    @PostMapping("/prepare")
    public ApiResponse<LiveRaceSnapshotResponse> prepare(@PathVariable UUID raceId) {
        return ApiResponse.<LiveRaceSnapshotResponse>builder()
                .result(lifecycleService.prepare(raceId)).build();
    }

    @GetMapping("/warnings")
    public ApiResponse<List<SimulationWarningResponse>> warnings(@PathVariable UUID raceId) {
        return ApiResponse.<List<SimulationWarningResponse>>builder()
                .result(incidentService.warnings(raceId)).build();
    }

    @GetMapping("/flags")
    public ApiResponse<List<SimulationFlagResponse>> flags(@PathVariable UUID raceId) {
        return ApiResponse.<List<SimulationFlagResponse>>builder()
                .result(incidentService.flags(raceId)).build();
    }

    @PostMapping("/warnings/{warningId}/ignore")
    public ApiResponse<SimulationWarningResponse> ignore(
            @PathVariable UUID raceId,
            @PathVariable UUID warningId,
            @RequestBody(required = false) @Valid IncidentReviewRequest request) {
        return ApiResponse.<SimulationWarningResponse>builder()
                .result(incidentService.ignore(raceId, warningId, request)).build();
    }

    @PostMapping("/warnings/{warningId}/flag")
    public ApiResponse<SimulationFlagResponse> flagWarning(
            @PathVariable UUID raceId,
            @PathVariable UUID warningId,
            @RequestBody(required = false) @Valid IncidentReviewRequest request) {
        return ApiResponse.<SimulationFlagResponse>builder()
                .result(incidentService.flagWarning(raceId, warningId, request)).build();
    }

    @PostMapping("/flags/manual")
    public ApiResponse<SimulationFlagResponse> manualFlag(
            @PathVariable UUID raceId,
            @RequestBody @Valid ManualFlagRequest request) {
        return ApiResponse.<SimulationFlagResponse>builder()
                .result(incidentService.manualFlag(raceId, request)).build();
    }

    @PostMapping("/flags/{flagId}/dismiss")
    public ApiResponse<SimulationFlagResponse> dismiss(
            @PathVariable UUID raceId,
            @PathVariable UUID flagId,
            @RequestBody(required = false) @Valid IncidentReviewRequest request) {
        return ApiResponse.<SimulationFlagResponse>builder()
                .result(incidentService.reviewFlag(raceId, flagId, request, false)).build();
    }

    @PostMapping("/flags/{flagId}/confirm")
    public ApiResponse<SimulationFlagResponse> confirm(
            @PathVariable UUID raceId,
            @PathVariable UUID flagId,
            @RequestBody(required = false) @Valid IncidentReviewRequest request) {
        return ApiResponse.<SimulationFlagResponse>builder()
                .result(incidentService.reviewFlag(raceId, flagId, request, true)).build();
    }

    @GetMapping("/provisional-results")
    public ApiResponse<List<ProvisionalResultResponse>> provisionalResults(@PathVariable UUID raceId) {
        accessService.requireAssignedReferee(raceId);
        return ApiResponse.<List<ProvisionalResultResponse>>builder()
                .result(queryService.provisionalResults(raceId, true)).build();
    }
}
