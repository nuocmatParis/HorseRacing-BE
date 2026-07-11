package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.jockeyinspection.request.JockeyInspectionRequest;
import com.swp391.horseracing.dto.jockeyinspection.response.JockeyInspectionResponse;
import com.swp391.horseracing.service.JockeyInspectionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/medical/race-entries")
@PreAuthorize("hasRole('MEDICAL_STAFF')")
public class MedicalInspectionController {

    JockeyInspectionService jockeyInspectionService;

    @PostMapping("/{entryId}/jockey-inspection")
    public ApiResponse<JockeyInspectionResponse> createJockeyInspection(
            @PathVariable UUID entryId,
            @RequestBody @Valid JockeyInspectionRequest request) {
        return ApiResponse.<JockeyInspectionResponse>builder()
                .result(jockeyInspectionService.createInspection(entryId, request))
                .build();
    }
}
