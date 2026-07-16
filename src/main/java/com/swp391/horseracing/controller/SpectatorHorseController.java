package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.spectator.response.SpectatorHorseResponse;
import com.swp391.horseracing.service.SpectatorHorseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/spectator/horses")
@PreAuthorize("hasRole('SPECTATOR')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpectatorHorseController {
    SpectatorHorseService spectatorHorseService;

    @GetMapping
    public ApiResponse<PageResponse<SpectatorHorseResponse>> searchHorses(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String raceClass,
            @RequestParam(required = false) String healthStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<SpectatorHorseResponse>>builder()
                .result(spectatorHorseService.searchHorses(query, raceClass, healthStatus, page, size))
                .build();
    }

    @GetMapping("/following")
    public ApiResponse<PageResponse<SpectatorHorseResponse>> getFollowingHorses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<SpectatorHorseResponse>>builder()
                .result(spectatorHorseService.getFollowingHorses(page, size))
                .build();
    }

    @PostMapping("/{horseId}/follow")
    public ApiResponse<SpectatorHorseResponse> followHorse(@PathVariable UUID horseId) {
        return ApiResponse.<SpectatorHorseResponse>builder()
                .result(spectatorHorseService.followHorse(horseId))
                .build();
    }

    @DeleteMapping("/{horseId}/follow")
    public ApiResponse<Void> unfollowHorse(@PathVariable UUID horseId) {
        spectatorHorseService.unfollowHorse(horseId);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/{horseId}")
    public ApiResponse<SpectatorHorseResponse> getHorseDetail(@PathVariable UUID horseId) {
        return ApiResponse.<SpectatorHorseResponse>builder()
                .result(spectatorHorseService.getHorseDetail(horseId))
                .build();
    }
}
