package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.prediction.response.AIPredictionResponse;
import com.swp391.horseracing.service.AIPredictionService;
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
public class AIPredictionController {

    AIPredictionService aiPredictionService;

    @PostMapping("/api/admin/races/{raceId}/ai-predictions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AIPredictionResponse>> generatePredictions(
            @PathVariable UUID raceId,
            @RequestParam(defaultValue = "3") int topN) {
        return ApiResponse.<List<AIPredictionResponse>>builder()
                .result(aiPredictionService.generatePredictions(raceId, topN))
                .build();
    }

    @GetMapping("/api/admin/races/{raceId}/ai-predictions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AIPredictionResponse>> getPredictionsByRace(@PathVariable UUID raceId) {
        return ApiResponse.<List<AIPredictionResponse>>builder()
                .result(aiPredictionService.getPredictionsByRace(raceId))
                .build();
    }

    @GetMapping("/api/spectator/races/{raceId}/ai-predictions")
    @PreAuthorize("hasRole('SPECTATOR')")
    public ApiResponse<List<AIPredictionResponse>> getSpectatorPredictionsByRace(@PathVariable UUID raceId) {
        return ApiResponse.<List<AIPredictionResponse>>builder()
                .result(aiPredictionService.getPredictionsByRace(raceId))
                .build();
    }
}
