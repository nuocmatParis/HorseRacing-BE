package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.jockey.response.JockeyStatisticsResponse;
import com.swp391.horseracing.service.JockeyStatisticsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jockey")
@PreAuthorize("hasRole('JOCKEY')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JockeyStatisticsController {
    JockeyStatisticsService jockeyStatisticsService;

    @GetMapping("/statistics")
    public ApiResponse<JockeyStatisticsResponse> getMyStatistics() {
        return ApiResponse.<JockeyStatisticsResponse>builder()
                .result(jockeyStatisticsService.getMyStatistics())
                .build();
    }
}
