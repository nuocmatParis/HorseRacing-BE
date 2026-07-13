package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.common.PageResponse;
import com.swp391.horseracing.dto.race_portal.AssignedRaceResponse;
import com.swp391.horseracing.service.RacePortalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AssignedRaceController {
    RacePortalService racePortalService;

    @GetMapping("/api/referee/races/assigned")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<PageResponse<AssignedRaceResponse>> getRefereeRaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<AssignedRaceResponse>>builder()
                .result(racePortalService.getRefereeAssignedRaces(page, size))
                .build();
    }

    @GetMapping("/api/vet/races/assigned")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<PageResponse<AssignedRaceResponse>> getVeterinarianRaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<AssignedRaceResponse>>builder()
                .result(racePortalService.getVeterinarianAssignedRaces(page, size))
                .build();
    }

    @GetMapping("/api/medical/races/assigned")
    @PreAuthorize("hasRole('MEDICAL_STAFF')")
    public ApiResponse<PageResponse<AssignedRaceResponse>> getMedicalRaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<AssignedRaceResponse>>builder()
                .result(racePortalService.getMedicalAssignedRaces(page, size))
                .build();
    }
}
