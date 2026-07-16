package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.prediction.response.AIPredictionAggregateResponse;
import com.swp391.horseracing.service.AIPredictionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AIPredictionController {

    AIPredictionService aiPredictionService;

    @PostMapping({
            "/api/admin/races/{raceId}/ai-predictions",
            "/api/admin/races/{raceId}/ai-predictions/generate"
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AIPredictionAggregateResponse> generatePredictions(
            @PathVariable UUID raceId,
            @RequestParam(defaultValue = "3") int topN) {
        return ApiResponse.<AIPredictionAggregateResponse>builder()
                .result(aiPredictionService.generatePredictions(raceId, topN))
                .build();
    }

    @GetMapping("/api/admin/races/{raceId}/ai-predictions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AIPredictionAggregateResponse> getPredictionsByRace(@PathVariable UUID raceId) {
        return ApiResponse.<AIPredictionAggregateResponse>builder()
                .result(aiPredictionService.getAdminPredictionsByRace(raceId))
                .build();
    }

    @PostMapping("/api/admin/races/{raceId}/ai-predictions/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AIPredictionAggregateResponse> publishPredictions(@PathVariable UUID raceId) {
        return ApiResponse.<AIPredictionAggregateResponse>builder()
                .result(aiPredictionService.publishPredictions(raceId))
                .build();
    }

    @PostMapping("/api/admin/races/{raceId}/ai-predictions/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AIPredictionAggregateResponse> unpublishPredictions(@PathVariable UUID raceId) {
        return ApiResponse.<AIPredictionAggregateResponse>builder()
                .result(aiPredictionService.unpublishPredictions(raceId))
                .build();
    }

    @GetMapping("/api/spectator/races/{raceId}/ai-predictions")
    @PreAuthorize("hasRole('SPECTATOR')")
    public ApiResponse<AIPredictionAggregateResponse> getSpectatorPredictionsByRace(@PathVariable UUID raceId) {
        return ApiResponse.<AIPredictionAggregateResponse>builder()
                .result(aiPredictionService.getPublishedPredictionsByRace(raceId))
                .build();
    }
}
