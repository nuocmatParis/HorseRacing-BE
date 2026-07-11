package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.prediction.request.CreatePredictionRequest;
import com.swp391.horseracing.dto.prediction.request.UpdatePredictionRequest;
import com.swp391.horseracing.dto.prediction.response.PredictionResponse;
import com.swp391.horseracing.service.PredictionService;
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
public class SpectatorPredictionController {

    PredictionService predictionService;

    @PostMapping("/api/spectator/races/{raceId}/predictions")
    @PreAuthorize("hasRole('SPECTATOR')")
    public ApiResponse<PredictionResponse> createPrediction(@PathVariable UUID raceId,
                                                             @RequestBody @Valid CreatePredictionRequest request) {
        return ApiResponse.<PredictionResponse>builder()
                .result(predictionService.createPrediction(raceId, request))
                .build();
    }

    @PutMapping("/api/spectator/predictions/{predictionId}")
    @PreAuthorize("hasRole('SPECTATOR')")
    public ApiResponse<PredictionResponse> updatePrediction(@PathVariable UUID predictionId,
                                                            @RequestBody @Valid UpdatePredictionRequest request) {
        return ApiResponse.<PredictionResponse>builder()
                .result(predictionService.updatePrediction(predictionId, request))
                .build();
    }

    @DeleteMapping("/api/spectator/predictions/{predictionId}")
    @PreAuthorize("hasRole('SPECTATOR')")
    public ApiResponse<Void> cancelPrediction(@PathVariable UUID predictionId) {
        predictionService.cancelPrediction(predictionId);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/api/spectator/predictions")
    @PreAuthorize("hasRole('SPECTATOR')")
    public ApiResponse<List<PredictionResponse>> getMyPredictions() {
        return ApiResponse.<List<PredictionResponse>>builder()
                .result(predictionService.getMyPredictions())
                .build();
    }

    @GetMapping("/api/spectator/predictions/{predictionId}")
    @PreAuthorize("hasRole('SPECTATOR')")
    public ApiResponse<PredictionResponse> getPredictionDetail(@PathVariable UUID predictionId) {
        return ApiResponse.<PredictionResponse>builder()
                .result(predictionService.getPredictionDetail(predictionId))
                .build();
    }

    @GetMapping("/api/races/{raceId}/predictions/me")
    @PreAuthorize("hasRole('SPECTATOR')")
    public ApiResponse<PredictionResponse> getMyPredictionByRace(@PathVariable UUID raceId) {
        return ApiResponse.<PredictionResponse>builder()
                .result(predictionService.getMyPredictionByRace(raceId))
                .build();
    }
}
