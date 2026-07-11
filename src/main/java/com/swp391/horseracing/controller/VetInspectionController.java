package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.horseinspection.request.HorseInspectionRequest;
import com.swp391.horseracing.dto.horseinspection.response.HorseInspectionResponse;
import com.swp391.horseracing.service.HorseInspectionService;
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
@RequestMapping("/api/vet/race-entries")
@PreAuthorize("hasRole('VETERINARIAN')")
public class VetInspectionController {

    HorseInspectionService horseInspectionService;

    @PostMapping("/{entryId}/horse-inspection")
    public ApiResponse<HorseInspectionResponse> createHorseInspection(
            @PathVariable UUID entryId,
            @RequestBody @Valid HorseInspectionRequest request) {
        return ApiResponse.<HorseInspectionResponse>builder()
                .result(horseInspectionService.createInspection(entryId, request))
                .build();
    }
}
