package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.appeal_evidence.request.AddAppealEvidenceRequest;
import com.swp391.horseracing.dto.appeal_evidence.response.AppealEvidenceResponse;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.service.AppealEvidenceService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.swp391.horseracing.enums.AppealEvidenceType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppealEvidenceController {

    AppealEvidenceService appealEvidenceService;

    @PostMapping("/api/appeals/{appealId}/evidences")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<AppealEvidenceResponse> addEvidence(
            @PathVariable UUID appealId,
            @RequestBody @Valid AddAppealEvidenceRequest request) {
        return ApiResponse.<AppealEvidenceResponse>builder()
                .result(appealEvidenceService.addEvidence(appealId, request))
                .build();
    }

    @GetMapping("/api/appeals/{appealId}/evidences")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY', 'REFEREE')")
    public ApiResponse<List<AppealEvidenceResponse>> getEvidences(@PathVariable UUID appealId) {
        return ApiResponse.<List<AppealEvidenceResponse>>builder()
                .result(appealEvidenceService.getEvidencesByAppealId(appealId))
                .build();
    }

    @PutMapping("/api/appeals/{appealId}/evidences/{evidenceId}")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<AppealEvidenceResponse> updateEvidence(
            @PathVariable UUID appealId,
            @PathVariable UUID evidenceId,
            @RequestBody @Valid AddAppealEvidenceRequest request) {
        return ApiResponse.<AppealEvidenceResponse>builder()
                .result(appealEvidenceService.updateEvidence(appealId, evidenceId, request))
                .build();
    }

    @DeleteMapping("/api/appeals/{appealId}/evidences/{evidenceId}")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<Void> deleteEvidence(
            @PathVariable UUID appealId,
            @PathVariable UUID evidenceId) {
        appealEvidenceService.deleteEvidence(appealId, evidenceId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping(value = "/api/appeals/{appealId}/evidences/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<AppealEvidenceResponse> uploadEvidence(
            @PathVariable UUID appealId,
            @RequestParam AppealEvidenceType type,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description) {
        return ApiResponse.<AppealEvidenceResponse>builder()
                .result(appealEvidenceService.uploadEvidence(appealId, type, file, description))
                .build();
    }
}
