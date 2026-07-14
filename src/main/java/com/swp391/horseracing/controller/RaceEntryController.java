package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.race_entry.request.CreateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.request.UpdateLaneRequest;
import com.swp391.horseracing.dto.race_entry.request.UpdateRaceEntryRequest;
import com.swp391.horseracing.dto.race_entry.response.RaceEntryResponse;
import com.swp391.horseracing.service.RaceEntryService;
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
@RequestMapping("/api/race-entries")
public class RaceEntryController {

    RaceEntryService raceEntryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RaceEntryResponse> create(@RequestBody @Valid CreateRaceEntryRequest request) {
        return ApiResponse.<RaceEntryResponse>builder()
                .result(raceEntryService.create(request))
                .build();
    }

    @PutMapping("/{entryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RaceEntryResponse> updateStatus(@PathVariable UUID entryId,
                                                        @RequestBody @Valid UpdateRaceEntryRequest request) {
        return ApiResponse.<RaceEntryResponse>builder()
                .result(raceEntryService.updateStatus(entryId, request))
                .build();
    }

    @GetMapping("/race/{raceId}")
    public ApiResponse<List<RaceEntryResponse>> getEntriesByRace(@PathVariable UUID raceId) {
        return ApiResponse.<List<RaceEntryResponse>>builder()
                .result(raceEntryService.getEntriesByRaceId(raceId))
                .build();
    }

    @GetMapping("/{entryId}")
    public ApiResponse<RaceEntryResponse> getEntryById(@PathVariable UUID entryId) {
        return ApiResponse.<RaceEntryResponse>builder()
                .result(raceEntryService.getEntryById(entryId))
                .build();
    }

    @DeleteMapping("/{entryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID entryId) {
        raceEntryService.delete(entryId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/rounds/{roundId}/auto-assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> autoAssignRound(@PathVariable UUID roundId) {
        raceEntryService.autoAssignRound(roundId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/races/{raceId}/auto-assign-lanes")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> autoAssignLanes(@PathVariable UUID raceId) {
        raceEntryService.autoAssignLanes(raceId);
        return ApiResponse.<Void>builder().build();
    }

    @PatchMapping("/{entryId}/lane")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RaceEntryResponse> updateLane(@PathVariable UUID entryId,
                                                      @RequestBody @Valid UpdateLaneRequest request) {
        return ApiResponse.<RaceEntryResponse>builder()
                .result(raceEntryService.updateLane(entryId, request.getLaneNumber()))
                .build();
    }

    @PatchMapping("/{entryId}/swap/{targetEntryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RaceEntryResponse> swapLanes(@PathVariable UUID entryId,
                                                     @PathVariable UUID targetEntryId) {
        return ApiResponse.<RaceEntryResponse>builder()
                .result(raceEntryService.swapLanes(entryId, targetEntryId))
                .build();
    }
}
