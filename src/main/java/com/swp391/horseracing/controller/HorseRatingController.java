package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.horse.HorseRatingHistoryResponse;
import com.swp391.horseracing.dto.horse.RaceRatingChangesResponse;
import com.swp391.horseracing.dto.horse.RaceRatingPreviewResponse;
import com.swp391.horseracing.dto.horse.RoundRatingSummaryResponse;
import com.swp391.horseracing.service.HorseRatingService;
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
public class HorseRatingController {

    HorseRatingService horseRatingService;

    @GetMapping("/api/admin/races/{raceId}/rating-preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RaceRatingPreviewResponse> getRatingPreview(@PathVariable UUID raceId) {
        return ApiResponse.<RaceRatingPreviewResponse>builder()
                .result(horseRatingService.previewForRace(raceId))
                .build();
    }

    @GetMapping("/api/admin/races/{raceId}/rating-changes")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RaceRatingChangesResponse> getRatingChanges(@PathVariable UUID raceId) {
        return ApiResponse.<RaceRatingChangesResponse>builder()
                .result(horseRatingService.getRatingChangesForRace(raceId))
                .build();
    }

    @GetMapping("/api/horses/{horseId}/rating-history")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'ADMIN')")
    public ApiResponse<List<HorseRatingHistoryResponse>> getHorseRatingHistory(@PathVariable UUID horseId) {
        return ApiResponse.<List<HorseRatingHistoryResponse>>builder()
                .result(horseRatingService.getRatingHistoryForHorse(horseId))
                .build();
    }

    @GetMapping("/api/admin/rounds/{roundId}/rating-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RoundRatingSummaryResponse> getRoundRatingSummary(@PathVariable UUID roundId) {
        return ApiResponse.<RoundRatingSummaryResponse>builder()
                .result(horseRatingService.getRoundRatingSummary(roundId))
                .build();
    }
}
