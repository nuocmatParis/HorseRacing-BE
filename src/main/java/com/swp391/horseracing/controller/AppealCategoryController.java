package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.appeal_category.request.CreateAppealCategoryRequest;
import com.swp391.horseracing.dto.appeal_category.response.AppealCategoryResponse;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.service.AppealCategoryService;
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
@RequestMapping("/api/appeal-categories")
public class AppealCategoryController {

    AppealCategoryService appealCategoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AppealCategoryResponse> create(@RequestBody @Valid CreateAppealCategoryRequest request) {
        return ApiResponse.<AppealCategoryResponse>builder()
                .result(appealCategoryService.create(request))
                .build();
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AppealCategoryResponse> update(@PathVariable UUID categoryId,
                                                       @RequestBody @Valid CreateAppealCategoryRequest request) {
        return ApiResponse.<AppealCategoryResponse>builder()
                .result(appealCategoryService.update(categoryId, request))
                .build();
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<AppealCategoryResponse> getById(@PathVariable UUID categoryId) {
        return ApiResponse.<AppealCategoryResponse>builder()
                .result(appealCategoryService.getById(categoryId))
                .build();
    }

    @GetMapping
    public ApiResponse<List<AppealCategoryResponse>> getAll() {
        return ApiResponse.<List<AppealCategoryResponse>>builder()
                .result(appealCategoryService.getAll())
                .build();
    }

    @PatchMapping("/{categoryId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> toggleActive(@PathVariable UUID categoryId) {
        appealCategoryService.toggleActive(categoryId);
        return ApiResponse.<Void>builder().build();
    }
}
