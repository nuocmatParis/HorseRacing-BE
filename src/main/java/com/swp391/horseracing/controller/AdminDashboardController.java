package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.admin.response.AdminDashboardSummaryResponse;
import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.service.AdminDashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    public ApiResponse<AdminDashboardSummaryResponse> getSummary() {
        return ApiResponse.<AdminDashboardSummaryResponse>builder()
                .result(adminDashboardService.getSummary())
                .build();
    }
}
