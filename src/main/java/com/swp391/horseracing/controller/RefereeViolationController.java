package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.violation.request.ViolationCreateRequest;
import com.swp391.horseracing.dto.violation.response.ViolationResponse;
import com.swp391.horseracing.service.ViolationService;
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
public class RefereeViolationController {

    ViolationService violationService;

    @PostMapping("/api/referee/race-entries/{entryId}/violations")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<ViolationResponse> createViolation(
            @PathVariable UUID entryId,
            @RequestBody @Valid ViolationCreateRequest request) {
        return ApiResponse.<ViolationResponse>builder()
                .result(violationService.createViolation(entryId, request))
                .build();
    }

    @GetMapping("/api/races/{raceId}/violations")
    public ApiResponse<List<ViolationResponse>> getViolationsByRaceId(@PathVariable UUID raceId) {
        return ApiResponse.<List<ViolationResponse>>builder()
                .result(violationService.getViolationsByRaceId(raceId))
                .build();
    }
}
