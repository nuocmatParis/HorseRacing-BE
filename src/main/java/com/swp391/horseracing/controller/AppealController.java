package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.appeal.request.CreateAppealRequest;
import com.swp391.horseracing.dto.appeal.request.ReviewAppealRequest;
import com.swp391.horseracing.dto.appeal.response.AppealResponse;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.service.AppealService;
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
public class AppealController {

    AppealService appealService;

    @PostMapping("/api/appeals")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<AppealResponse> create(@RequestBody @Valid CreateAppealRequest request) {
        return ApiResponse.<AppealResponse>builder()
                .result(appealService.create(request))
                .build();
    }

    @PutMapping("/api/appeals/{appealId}")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<AppealResponse> update(@PathVariable UUID appealId,
                                               @RequestBody @Valid CreateAppealRequest request) {
        return ApiResponse.<AppealResponse>builder()
                .result(appealService.update(appealId, request))
                .build();
    }

    @DeleteMapping("/api/appeals/{appealId}")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<Void> cancel(@PathVariable UUID appealId) {
        appealService.cancel(appealId);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/api/appeals/my")
    @PreAuthorize("hasAnyRole('HORSE_OWNER', 'JOCKEY')")
    public ApiResponse<List<AppealResponse>> getMyAppeals() {
        return ApiResponse.<List<AppealResponse>>builder()
                .result(appealService.getMyAppeals())
                .build();
    }

    @GetMapping("/api/referee/appeals")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<List<AppealResponse>> getAllAppeals() {
        return ApiResponse.<List<AppealResponse>>builder()
                .result(appealService.getAllAppeals())
                .build();
    }

    @GetMapping("/api/referee/appeals/{appealId}")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<AppealResponse> getAppealDetail(@PathVariable UUID appealId) {
        return ApiResponse.<AppealResponse>builder()
                .result(appealService.getAppealDetail(appealId))
                .build();
    }

    @PostMapping("/api/referee/appeals/{appealId}/review")
    @PreAuthorize("hasRole('REFEREE')")
    public ApiResponse<AppealResponse> review(@PathVariable UUID appealId,
                                               @RequestBody @Valid ReviewAppealRequest request) {
        return ApiResponse.<AppealResponse>builder()
                .result(appealService.review(appealId, request))
                .build();
    }
}
